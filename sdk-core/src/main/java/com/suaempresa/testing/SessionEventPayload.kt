package com.suaempresa.testing

/**
 * Representa um evento enviado pelo SDK ao backend.
 */
internal data class SessionEventPayload(
    val packageName: String,
    val deviceId: String,
    val eventType: String,
    val durationSeconds: Long = 0,
    val referrer: String? = null,
    val installSource: String =
        InstallSourceDetector.UNKNOWN
) {

    init {
        require(packageName.isNotBlank()) {
            "packageName não pode estar vazio."
        }

        require(deviceId.isNotBlank()) {
            "deviceId não pode estar vazio."
        }

        require(
            eventType == SESSION_START ||
                eventType == SESSION_END
        ) {
            "eventType deve ser session_start ou session_end."
        }

        require(durationSeconds >= 0) {
            "durationSeconds não pode ser negativo."
        }

        require(
            installSource in
                INSTALL_SOURCES_PERMITIDAS
        ) {
            "installSource inválido."
        }
    }

    companion object {
        const val SESSION_START =
            "session_start"

        const val SESSION_END =
            "session_end"

        private val INSTALL_SOURCES_PERMITIDAS =
            setOf(
                InstallSourceDetector.GOOGLE_PLAY,
                InstallSourceDetector.DEVELOPMENT,
                InstallSourceDetector.EXTERNAL,
                InstallSourceDetector.UNKNOWN
            )
    }
}