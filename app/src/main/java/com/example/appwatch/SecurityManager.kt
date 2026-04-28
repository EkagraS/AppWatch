package com.example.appwatch

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getDatabasePassphrase(): ByteArray {
        var key = sharedPrefs.getString("db_passphrase", null)

        if (key == null) {
            // 1. SecureRandom use karke 32 bytes generate karo
            val random = SecureRandom()
            val bytes = ByteArray(32)
            random.nextBytes(bytes)

            // 2. Isey Hex ya Base64 mein convert karke save karo
            key = bytes.joinToString("") { "%02x".format(it) }
            sharedPrefs.edit().putString("db_passphrase", key).apply()
        }

        return key.toByteArray()
    }
}