package com.suaempresa.testing

import android.content.Context
import androidx.startup.Initializer

class SDKInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        TestingSDK.start(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
