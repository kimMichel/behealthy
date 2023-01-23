package com.example.behealthy.shared.security

import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.spec.IvParameterSpec

class IvManager(private val pref: SharedPreferences) {

    var iv: IvParameterSpec

    init {
        val random = if (pref.contains(Encryption22.IV_KEY)) {
            val base64 = pref.getString(Encryption22.IV_KEY, null)!!
            Base64.decode(base64, Base64.DEFAULT)
        } else {
            newStoredRandom()
        }
        iv = IvParameterSpec(random)
    }

    fun reload() {
        val random = newStoredRandom()
        iv = IvParameterSpec(random)
    }

    private fun generateSecureRandom(): ByteArray {
        val key = ByteArray(16)
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(key)
        return key
    }

    private fun newStoredRandom(): ByteArray? {
        val newRandom = generateSecureRandom()
        val stringValue = Base64.encodeToString(newRandom, Base64.DEFAULT)
        pref.edit().putString(Encryption22.IV_KEY, stringValue).apply()
        return newRandom
    }
}
