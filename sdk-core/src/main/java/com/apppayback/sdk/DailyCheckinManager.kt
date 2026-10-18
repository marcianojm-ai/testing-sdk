package com.apppayback.sdk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Serializa rede e persistência em um único executor. O servidor continua sendo
 * a autoridade sobre duração, continuidade e validação do dia.
 */
internal class DailyCheckinManager(
    private val app: Application
) : Application.ActivityLifecycleCallbacks {
    private val gate = CheckinContinuity()
    private val foregroundTracker = ActivityForegroundTracker()
    private val network = app.getSystemService(
        Context.CONNECTIVITY_SERVICE
    ) as ConnectivityManager
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val store = ParticipationStore(app)
    private val api = CheckinApiClient()
    private val main = Handler(Looper.getMainLooper())
    private val backoff = RequestBackoff()
    private val messages = CheckinMessages(app, main)
    private val clientInfo: ClientInfo by lazy { ClientInfo.read(app) }
    private val schedulingLock = Any()

    private var state: JSONObject? = null
    private var identity: JSONObject? = null
    private var activeGeneration = -1L
    private var suppressedGeneration = -1L
    private var nextRequestAt = 0L
    private var sequence = 1L
    private var canFinish = false
    private var heartbeatIntervalMs = DEFAULT_HEARTBEAT_INTERVAL_MS
    private var scheduledTick: ScheduledFuture<*>? = null

    @Volatile
    private var disabled = false

    private val tokenPattern = Regex("^[A-Za-z0-9_-]{43}$")

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refreshNetwork()

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) = refreshNetwork()

            override fun onLost(network: Network) = refreshNetwork()
        }

    fun attach() {
        app.registerActivityLifecycleCallbacks(this)

        try {
            network.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build(),
                networkCallback
            )
            refreshNetwork()
        } catch (_: Exception) {
            disabled = true
            Log.w(
                TAG,
                "Monitoramento de rede indisponível. Check-in desativado."
            )
        }

        if (disabled) return

        wakeScheduler()
    }

    private fun refreshNetwork() {
        val connected = try {
            val capabilities = network.getNetworkCapabilities(
                network.activeNetwork
            )
            capabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) == true && capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )
        } catch (_: Exception) {
            false
        }

        val wasConnected = gate.connected
        gate.setConnected(connected)

        if (connected && !wasConnected) {
            nextRequestAt = 0L
            wakeScheduler()
        } else if (!connected) {
            cancelScheduledTick()
        }
    }

    /**
     * Agenda somente o próximo trabalho necessário. Assim o SDK não mantém um
     * ticker acordando o processo enquanto está em background ou ocioso.
     */
    private fun wakeScheduler(delayMs: Long = 0L) {
        if (
            disabled ||
            !gate.foreground ||
            !gate.connected
        ) {
            cancelScheduledTick()
            return
        }

        val safeDelay = delayMs.coerceAtLeast(0L)
        synchronized(schedulingLock) {
            scheduledTick?.cancel(false)
            scheduledTick = executor.schedule(
                {
                    try {
                        tick()
                    } catch (_: Exception) {
                        if (!disabled) {
                            scheduleFailure()
                            Log.w(
                                TAG,
                                "Check-in indisponível; aguardando nova tentativa."
                            )
                        }
                    }

                    scheduleNextTick()
                },
                safeDelay,
                TimeUnit.MILLISECONDS
            )
        }
    }

    private fun scheduleNextTick() {
        if (
            disabled ||
            !gate.foreground ||
            !gate.connected ||
            suppressedGeneration == gate.generation
        ) {
            return
        }

        val remaining =
            nextRequestAt - SystemClock.elapsedRealtime()
        wakeScheduler(
            remaining.coerceAtLeast(MIN_SCHEDULER_DELAY_MS)
        )
    }

    private fun cancelScheduledTick() {
        synchronized(schedulingLock) {
            scheduledTick?.cancel(false)
            scheduledTick = null
        }
    }

    private fun load(): JSONObject {
        state?.let { return it }

        try {
            val loaded = store.read()
            val installationId = IdentityManager(app)
                .getOrCreateInstallationId()
            require(
                !loaded.has(INSTALLATION_ID) ||
                    loaded.getString(INSTALLATION_ID) == installationId
            )

            val fingerprint = AppFingerprintProvider(app)
            identity = JSONObject()
                .put(PACKAGE_NAME, app.packageName)
                .put(INSTALLATION_ID, installationId)
                .put(APP_FINGERPRINT, fingerprint.getAppFingerprint())
            loaded.put(INSTALLATION_ID, installationId)
            state = loaded
            return loaded
        } catch (error: Exception) {
            disabled = true
            throw error
        }
    }

    private fun body(): JSONObject =
        JSONObject(requireNotNull(identity).toString())

    private fun save(data: JSONObject) {
        try {
            store.save(data)
        } catch (error: Exception) {
            disabled = true
            throw error
        }
    }

    /** Persiste o convite somente no armazenamento criptografado. */
    fun acceptParticipationToken(token: String) {
        executor.execute {
            try {
                if (!tokenPattern.matches(token)) return@execute

                val data = load()
                if (data.has(PARTICIPATION_TOKEN)) return@execute
                if (
                    FailedInviteMemory.contains(
                        failedInvites(data),
                        token
                    )
                ) {
                    return@execute
                }

                val currentToken = data.optString(PENDING_TOKEN)
                val currentCredential = data.optString(PENDING_CREDENTIAL)
                if (
                    currentToken == token &&
                    tokenPattern.matches(currentCredential)
                ) {
                    return@execute
                }

                data.put(PENDING_TOKEN, token)
                    .put(PENDING_CREDENTIAL, generateCredential())
                save(data)
                suppressedGeneration = -1L
                nextRequestAt = 0L
                backoff.reset()
                wakeScheduler()
            } catch (_: Exception) {
                Log.w(TAG, "Não foi possível preparar o convite da plataforma.")
            }
        }
    }

    /** API auxiliar para testes locais; o fluxo Play é automático. */
    fun redeem(
        token: String,
        callback: (String?) -> Unit
    ) {
        executor.execute {
            val result = try {
                if (!tokenPattern.matches(token)) {
                    "TOKEN_PARTICIPACAO_INVALIDO"
                } else {
                    val data = load()
                    if (data.has(PARTICIPATION_TOKEN)) {
                        "PARTICIPACAO_JA_EXISTENTE"
                    } else {
                        preparePending(data, token)
                        if (disabled || !gate.connected) {
                            "REDE_INDISPONIVEL"
                        } else {
                            redeemPending(data)
                        }
                    }
                }
            } catch (_: Exception) {
                scheduleFailure()
                "RESGATE_NAO_CONFIRMADO"
            }

            main.post { callback(result) }
            wakeScheduler()
        }
    }

    private fun preparePending(
        data: JSONObject,
        token: String
    ) {
        if (
            data.optString(PENDING_TOKEN) == token &&
            tokenPattern.matches(
                data.optString(PENDING_CREDENTIAL)
            )
        ) {
            return
        }

        data.put(PENDING_TOKEN, token)
            .put(PENDING_CREDENTIAL, generateCredential())
        save(data)
    }

    private fun generateCredential(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    private fun redeemPending(data: JSONObject): String? {
        val token = data.optString(PENDING_TOKEN)
        val credential = data.optString(PENDING_CREDENTIAL)

        if (
            !tokenPattern.matches(token) ||
            !tokenPattern.matches(credential)
        ) {
            clearPending(data)
            schedulePassive(PASSIVE_RETRY_MS)
            return "TOKEN_PARTICIPACAO_INVALIDO"
        }

        return try {
            val reply = api.post(
                "/participations/redeem",
                body()
                    .put("token", token)
                    .put("participation_credential", credential)
            )

            if (reply.successful) {
                val returnedCredential = reply.body.getString(
                    "participation_token"
                )
                require(returnedCredential == credential)

                val participationId = reply.body.getString(
                    "participation_id"
                )
                require(participationId.isNotBlank())

                val confirmed = JSONObject(data.toString())
                    .put(PARTICIPATION_TOKEN, credential)
                    .put(PARTICIPATION_ID, participationId)
                clearPending(confirmed, saveNow = false)
                save(confirmed)
                state = confirmed
                markSuccessfulRequest()
                null
            } else {
                val code = reply.errorCode.ifBlank {
                    "RESGATE_RECUSADO"
                }
                val terminal = ParticipationRedeemFailurePolicy
                    .shouldClearPending(reply.status, code)

                if (terminal) {
                    if (
                        FailedInviteMemory.shouldRemember(
                            reply.status,
                            code
                        )
                    ) {
                        rememberFailedInvite(data, token)
                    }
                    clearPending(data)
                    schedulePassive(PASSIVE_RETRY_MS)
                } else {
                    scheduleFailure(reply.retryAfterMs)
                }
                code
            }
        } catch (_: Exception) {
            // O mesmo token e a mesma credencial podem ser repetidos com segurança.
            scheduleFailure()
            "RESGATE_NAO_CONFIRMADO"
        }
    }

    private fun failedInvites(data: JSONObject): List<String> {
        val stored = data.optJSONArray(FAILED_INVITE_HASHES)
            ?: return emptyList()
        return (0 until stored.length())
            .map { index -> stored.optString(index) }
            .filter { it.isNotBlank() }
    }

    /** Gravado junto com a limpeza do convite pendente, em clearPending. */
    private fun rememberFailedInvite(
        data: JSONObject,
        token: String
    ) {
        val updated = FailedInviteMemory.remember(
            failedInvites(data),
            token
        )
        data.put(FAILED_INVITE_HASHES, JSONArray(updated))
    }

    private fun clearPending(
        data: JSONObject,
        saveNow: Boolean = true
    ) {
        data.remove(PENDING_TOKEN)
        data.remove(PENDING_CREDENTIAL)
        if (saveNow) save(data)
    }

    private fun tick() {
        val continuity = gate.snapshot()
        if (
            disabled ||
            !continuity.foreground ||
            !continuity.connected
        ) {
            return
        }

        val generation = continuity.generation
        if (suppressedGeneration == generation) return

        var data = load()
        val now = SystemClock.elapsedRealtime()

        if (isValidatedToday(data)) {
            if (now >= nextRequestAt) {
                nextRequestAt = now + VALIDATED_DAY_RECHECK_MS
            }
            return
        }

        if (now < nextRequestAt) return

        if (
            !data.has(PARTICIPATION_TOKEN) &&
            data.has(PENDING_TOKEN)
        ) {
            val result = redeemPending(data)
            activeGeneration = generation
            if (result != null) {
                Log.w(
                    TAG,
                    "Vínculo da participação ainda não foi confirmado."
                )
                return
            }
            data = load()
        }

        if (!data.has(PARTICIPATION_TOKEN)) {
            checkSdkAvailability(data, generation)
            return
        }

        val attempt = data.optJSONObject(ATTEMPT)
        if (
            activeGeneration != generation ||
            attempt == null
        ) {
            startOrRestart(data, generation, attempt)
            return
        }

        sendHeartbeatOrFinish(data, generation, attempt)
    }

    /**
     * sdk-status serve para a associação inicial do aplicativo. Uma participação
     * autenticada usa as próprias rotas de check-in, que revalidam app e conta.
     */
    private fun checkSdkAvailability(
        data: JSONObject,
        generation: Long
    ) {
        if (
            SdkStatusCachePolicy.shouldSkipCheck(
                lastSuccessAtMs = data.optLong(SDK_STATUS_OK_AT, 0L),
                cachedVersionCode = data.optLong(
                    SDK_STATUS_OK_VERSION_CODE,
                    -1L
                ),
                nowMs = System.currentTimeMillis(),
                currentVersionCode = clientInfo.appVersionCode
            )
        ) {
            suppressedGeneration = generation
            activeGeneration = -1L
            return
        }

        when (
            SdkStatusClient().checkBlocking(
                app.packageName,
                requireNotNull(identity).getString(APP_FINGERPRINT)
            )
        ) {
            SdkRemoteStatus.NEWLY_ASSOCIATED,
            SdkRemoteStatus.ACTIVE -> {
                markSuccessfulRequest()
                rememberSdkStatusSuccess(data)
                suppressedGeneration = generation
                activeGeneration = -1L
            }

            SdkRemoteStatus.INACTIVE -> {
                markSuccessfulRequest()
                schedulePassive(INACTIVE_RETRY_MS)
            }

            SdkRemoteStatus.UNAVAILABLE -> {
                scheduleFailure()
            }
        }
    }

    /** Uma falha ao gravar o cache não impede o funcionamento do SDK. */
    private fun rememberSdkStatusSuccess(data: JSONObject) {
        try {
            data.put(SDK_STATUS_OK_AT, System.currentTimeMillis())
                .put(SDK_STATUS_OK_VERSION_CODE, clientInfo.appVersionCode)
            store.save(data)
        } catch (_: Exception) {
            data.remove(SDK_STATUS_OK_AT)
            data.remove(SDK_STATUS_OK_VERSION_CODE)
        }
    }

    private fun startOrRestart(
        data: JSONObject,
        generation: Long,
        previousAttempt: JSONObject?
    ) {
        val request = clientInfo.applyTo(body())
        if (previousAttempt != null) {
            request.put(
                "reiniciar_attempt_id",
                previousAttempt.getString(ATTEMPT_ID)
            ).put(
                "reiniciar_data_teste",
                previousAttempt.getString(TEST_DATE)
            )
        }

        if (!gate.accepts(generation)) return

        val sentAt = SystemClock.elapsedRealtime()
        val credential = data.getString(PARTICIPATION_TOKEN)
        val reply = api.post(
            "/checkins/start",
            request,
            credential
        )

        if (!reply.successful) {
            handleCheckinError(
                reply = reply,
                data = data,
                duringStart = true
            )
            return
        }

        markSuccessfulRequest()
        messages.updateFromServer(reply.body)
        if (reply.body.optBoolean("ja_validado")) {
            markValidated(
                data,
                reply.body.getString(TEST_DATE)
            )
            return
        }

        heartbeatIntervalMs = validatedHeartbeatInterval(reply.body)
        val currentAttempt = JSONObject()
            .put(
                ATTEMPT_ID,
                reply.body.getString(ATTEMPT_ID)
            )
            .put(
                TEST_DATE,
                reply.body.getString(TEST_DATE)
            )
            .put(
                HEARTBEAT_INTERVAL,
                heartbeatIntervalMs
            )

        data.put(ATTEMPT, currentAttempt)
        save(data)
        sequence = 1L
        canFinish = false

        val activeAttempt =
            reply.body.optBoolean("iniciado") ||
                reply.body.optBoolean("ja_em_andamento")
        activeGeneration = if (
            gate.accepts(generation) && activeAttempt
        ) {
            generation
        } else {
            -1L
        }
        nextRequestAt = sentAt + heartbeatIntervalMs

        if (
            activeGeneration == generation &&
            reply.body.optBoolean("iniciado")
        ) {
            messages.showStarted()
        }
    }

    private fun sendHeartbeatOrFinish(
        data: JSONObject,
        generation: Long,
        attempt: JSONObject
    ) {
        heartbeatIntervalMs = attempt
            .optLong(
                HEARTBEAT_INTERVAL,
                DEFAULT_HEARTBEAT_INTERVAL_MS
            )
            .coerceIn(
                MIN_HEARTBEAT_INTERVAL_MS,
                MAX_HEARTBEAT_INTERVAL_MS
            )

        val request = body()
            .put(ATTEMPT_ID, attempt.getString(ATTEMPT_ID))
            .put(TEST_DATE, attempt.getString(TEST_DATE))
        if (!canFinish) request.put("heartbeat_seq", sequence)
        if (!gate.accepts(generation)) return

        val sentAt = SystemClock.elapsedRealtime()
        val reply = api.post(
            if (canFinish) {
                "/checkins/finish"
            } else {
                "/checkins/heartbeat"
            },
            request,
            data.getString(PARTICIPATION_TOKEN)
        )

        if (!reply.successful) {
            handleCheckinError(
                reply = reply,
                data = data,
                duringStart = false
            )
            return
        }

        markSuccessfulRequest()
        if (
            reply.body.optBoolean("validado") ||
            reply.body.optBoolean("ja_validado")
        ) {
            markValidated(
                data,
                reply.body.getString(TEST_DATE)
            )
            Log.d(TAG, "Check-in diário confirmado pelo servidor.")
            if (reply.body.optBoolean("validado")) {
                messages.showConfirmed()
            }
            return
        }

        if (!gate.accepts(generation)) {
            activeGeneration = -1L
            return
        }

        sequence = reply.body.getLong(
            "ultimo_heartbeat_seq"
        ) + 1L
        canFinish = reply.body.optBoolean("pode_finalizar")
        nextRequestAt = if (canFinish) {
            sentAt
        } else {
            sentAt + heartbeatIntervalMs
        }
    }

    private fun handleCheckinError(
        reply: CheckinReply,
        data: JSONObject,
        duringStart: Boolean
    ) {
        val errorCode = reply.errorCode

        when {
            CheckinRecoveryPolicy.shouldForgetAttempt(
                reply.status,
                errorCode,
                duringStart
            ) -> {
                forgetAttempt(data)
                backoff.reset()
                schedulePassive(QUICK_RECOVERY_MS)
            }

            CheckinRecoveryPolicy.isPermanentIdentityFailure(
                reply.status,
                errorCode
            ) -> {
                activeGeneration = -1L
                disabled = true
            }

            else -> {
                activeGeneration = -1L
                scheduleFailure(reply.retryAfterMs)
            }
        }

        Log.w(
            TAG,
            "Check-in recusado. HTTP ${reply.status}."
        )
    }

    private fun forgetAttempt(data: JSONObject) {
        data.remove(ATTEMPT)
        save(data)
        resetAttemptRuntime()
    }

    private fun markValidated(
        data: JSONObject,
        testDate: String
    ) {
        data.remove(ATTEMPT)
        data.put(LAST_VALIDATED_DATE, testDate)
        save(data)
        resetAttemptRuntime()
        backoff.reset()
        schedulePassive(VALIDATED_DAY_RECHECK_MS)
    }

    private fun isValidatedToday(data: JSONObject): Boolean {
        val validatedDate = data.optString(LAST_VALIDATED_DATE)
        if (validatedDate.isBlank()) return false

        if (validatedDate == localTestDate()) return true

        data.remove(LAST_VALIDATED_DATE)
        save(data)
        return false
    }

    private fun localTestDate(): String =
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
        }.format(System.currentTimeMillis())

    private fun validatedHeartbeatInterval(body: JSONObject): Long {
        val interval = body.optLong(
            "heartbeat_intervalo_ms",
            DEFAULT_HEARTBEAT_INTERVAL_MS
        )
        val tolerance = body.optLong(
            "heartbeat_tolerancia_ms",
            DEFAULT_HEARTBEAT_TOLERANCE_MS
        )

        return if (
            interval in MIN_HEARTBEAT_INTERVAL_MS..MAX_HEARTBEAT_INTERVAL_MS &&
            tolerance > interval
        ) {
            interval
        } else {
            DEFAULT_HEARTBEAT_INTERVAL_MS
        }
    }

    private fun resetAttemptRuntime() {
        activeGeneration = -1L
        sequence = 1L
        canFinish = false
        heartbeatIntervalMs = DEFAULT_HEARTBEAT_INTERVAL_MS
    }

    private fun markSuccessfulRequest() {
        backoff.reset()
    }

    private fun scheduleFailure(
        retryAfterMs: Long? = null
    ) {
        nextRequestAt = SystemClock.elapsedRealtime() +
            backoff.delayAfterFailure(retryAfterMs)
    }

    private fun schedulePassive(delayMs: Long) {
        nextRequestAt = SystemClock.elapsedRealtime() + delayMs
    }

    override fun onActivityStarted(activity: Activity) {
        foregroundTracker.onStarted()?.let { foreground ->
            gate.setForeground(foreground)
            nextRequestAt = 0L
            wakeScheduler()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        foregroundTracker.onStopped(
            activity.isChangingConfigurations
        )?.let { foreground ->
            gate.setForeground(foreground)
            if (!foreground) {
                cancelScheduledTick()
            }
        }
    }

    override fun onActivityCreated(
        activity: Activity,
        state: Bundle?
    ) = Unit

    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(
        activity: Activity,
        state: Bundle
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    companion object {
        private const val TAG = SdkConfig.LOG_TAG

        private const val PACKAGE_NAME = "package_name"
        private const val APP_FINGERPRINT = "app_fingerprint"
        private const val INSTALLATION_ID = "installation_id"
        private const val PARTICIPATION_TOKEN = "participation_token"
        private const val PARTICIPATION_ID = "participation_id"
        private const val PENDING_TOKEN = "pending_participation_token"
        private const val PENDING_CREDENTIAL =
            "pending_participation_credential"
        private const val ATTEMPT = "attempt"
        private const val ATTEMPT_ID = "attempt_id"
        private const val TEST_DATE = "data_teste"
        private const val HEARTBEAT_INTERVAL = "heartbeat_interval_ms"
        private const val LAST_VALIDATED_DATE = "last_validated_date"
        private const val SDK_STATUS_OK_AT = "sdk_status_ok_at"
        private const val SDK_STATUS_OK_VERSION_CODE =
            "sdk_status_ok_version_code"
        private const val FAILED_INVITE_HASHES = "failed_invite_hashes"

        private const val DEFAULT_HEARTBEAT_INTERVAL_MS = 10_000L
        private const val DEFAULT_HEARTBEAT_TOLERANCE_MS = 15_000L
        private const val MIN_HEARTBEAT_INTERVAL_MS = 5_000L
        private const val MAX_HEARTBEAT_INTERVAL_MS = 14_000L
        private const val QUICK_RECOVERY_MS = 1_000L
        private const val PASSIVE_RETRY_MS = 60_000L
        private const val INACTIVE_RETRY_MS = 5 * 60_000L
        private const val VALIDATED_DAY_RECHECK_MS = 60_000L
        private const val MIN_SCHEDULER_DELAY_MS = 100L
    }
}
