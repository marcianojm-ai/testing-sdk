package com.suaempresa.testing

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class IdentityManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "testing_sdk_preferences",
        Context.MODE_PRIVATE
    )

    fun getOrCreateInstallationId(): String {
        // Tenta buscar o ID que já existe
        var id = prefs.getString("installation_id", null)

        // Se não existir (primeira vez ou reinstalação), cria um novo e salva
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("installation_id", id).apply()
        }

        return id
    }
}