package com.suaempresa.testing

import android.content.Context
import android.util.Log

object TestingSDK {
    private const val TAG = "TestingSDK"

    fun start(context: Context) {
        Log.d(TAG, "SDK iniciado com sucesso!")
        // Aqui viria a lógica de envio de ping, etc.
    }
}