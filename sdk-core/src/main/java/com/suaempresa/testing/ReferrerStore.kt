package com.suaempresa.testing

import android.content.Context
import android.content.SharedPreferences

/**
 * Armazena localmente o Install Referrer obtido da Play Store.
 *
 * Isso permite que o SDK reutilize o referrer nos eventos de sessão,
 * mesmo depois que a conexão com a Play Store for encerrada.
 */
internal class ReferrerStore(context: Context) {

    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    fun save(referrer: String?) {
        if (referrer.isNullOrBlank()) {
            return
        }

        preferences
            .edit()
            .putString(
                KEY_INSTALL_REFERRER,
                referrer.trim()
            )
            .apply()
    }

    fun get(): String? {
        return preferences
            .getString(
                KEY_INSTALL_REFERRER,
                null
            )
            ?.takeIf { it.isNotBlank() }
    }

    /**
     * Remove um referrer antigo ou incompatível
     * com a origem atual da instalação.
     */
    fun clear() {
        preferences
            .edit()
            .remove(KEY_INSTALL_REFERRER)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME =
            "testing_sdk_preferences"

        private const val KEY_INSTALL_REFERRER =
            "install_referrer"
    }
}