package com.suaempresa.testing

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class SessionManager(private val deviceId: String) : DefaultLifecycleObserver {

    private var sessionStartTime: Long = 0

    // Disparado no exato milissegundo que o app aparece na tela do usuário
    override fun onStart(owner: LifecycleOwner) {
        sessionStartTime = System.currentTimeMillis()
        Log.d("TestingSDK", "--> session_start: O testador [$deviceId] abriu o app.")
    }

    // Disparado no exato milissegundo que o app é minimizado ou a tela é desligada
    override fun onStop(owner: LifecycleOwner) {
        if (sessionStartTime > 0) {
            val sessionDurationMillis = System.currentTimeMillis() - sessionStartTime
            val sessionDurationSeconds = sessionDurationMillis / 1000

            Log.d("TestingSDK", "<-- session_end: O testador [$deviceId] usou o app por $sessionDurationSeconds segundos.")
        }
    }
}