package com.example.behealthy.shared.security

import android.content.Context
import android.security.KeyPairGeneratorSpec
import android.util.Base64
import android.util.Log
import com.google.gson.GsonBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.Key
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal
import kotlin.reflect.KClass

class Encryption22(private val context: Context) : SecureStorage {

    private val pref get() = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE)

    private val ivManager = IvManager(pref)

    private val iv get() = ivManager.iv

    private val gson = GsonBuilder().run {
        setLenient()
        create()
    }

    private val keystore = KeyStore.getInstance(KEY_STORE).apply {
        load(null)
    }

    init {
        if (keystore.containsAlias(KEY_ALIAS)) {
            validateKeys()
        } else {
            configureKeys()
        }
    }

    private fun validateKeys() {
        try {
            get(TEST_KEY, String::class)
        } catch (e: java.lang.Exception) {
            Log.e("Encryption22", e.message.toString())
            pref.edit().clear().apply()
            keystore.deleteEntry(KEY_ALIAS)
            ivManager.reload()
            configureKeys()
        }
    }

    private fun configureKeys() {
        generateKeyPair()
        generateAES()
        put(TEST_KEY, "ok")
    }

    override fun put(key: String, value: Any) {
        val newValue: Any = when (value) {
            is Int -> value.toString()
            is Long -> value.toLong()
            is Float -> value.toFloat()
            else -> value
        }
        val json = gson.toJson(newValue)
        val encrypted = encrypt(json.toByteArray(charset= Charsets.UTF_8))
        pref.edit().putString(key, encrypted).apply()
    }

    override fun get(key: String, clazz: KClass<*>): Any? {
        if (pref.contains(key)) { return null }
        val encryptedB64Json = pref.getString(key, null) ?: return null
        val json = decrypt(encryptedB64Json)
        return when (clazz.java) {
            Int::class.java -> parseToString(json).toInt()
            java.lang.Integer::class.java -> parseToString(json).toInt()
            java.lang.Float::class.java -> parseToString(json).toFloat()
            java.lang.Long::class.java -> parseToString(json).toLong()
            else ->  gson.fromJson(json, clazz.java)
        }
    }

    private fun parseToString(json: String) = gson.fromJson(json, String::class.java)

    override fun delete(key: String) {
        pref.edit().remove(key).apply()
    }

    override fun contains(key: String): Boolean = pref.contains(key)

    private fun encrypt(input: ByteArray): String {
        val encodeBytes = Cipher.getInstance(AES_MODE).let {
            it.init(Cipher.DECRYPT_MODE, getSecretKey(), iv)
            val bytes = it.update(input)
            bytes + it.doFinal()
        }
        return Base64.encodeToString(encodeBytes, Base64.DEFAULT)
    }

    private fun decrypt(encryptedB64Json: String): String {
        val decoded = Base64.decode(encryptedB64Json, Base64.DEFAULT)
        val decrypted = Cipher.getInstance(AES_MODE).let {
            it.init(Cipher.DECRYPT_MODE, getSecretKey(), iv)
            val bytes = it.update(decoded)
            bytes + it.doFinal()
        }
        return String(decrypted, Charsets.UTF_8)
    }

    private fun getSecretKey(): Key {
        val encryptKeyB64 = pref.getString(ENCRYPTED_KEY, null)
            ?: throw IllegalAccessException("Cannot find encrypted key")
        val encryptedKey = Base64.decode(encryptKeyB64, Base64.DEFAULT)
        val key = rsaDecrypt(encryptedKey)
        return SecretKeySpec(key, "AES")
    }

    private fun generateAES() {
        val pref = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE)
        var encryptedKeyB64 = pref.getString(ENCRYPTED_KEY, null)

        if (encryptedKeyB64 == null) {
            val key = generateSecureRandom()

            val encryptedKey = rsaEncrypt(key)
            encryptedKeyB64 = Base64.encodeToString(encryptedKey, Base64.DEFAULT)

            val edit = pref.edit()
            edit.putString(ENCRYPTED_KEY, encryptedKeyB64)
            edit.apply()
        }
    }

    private fun generateKeyPair() {
        val start = Calendar.getInstance()
        val end = Calendar.getInstance()
        val amount = 30
        end.add(Calendar.YEAR, amount)

        val spec = KeyPairGeneratorSpec.Builder(context)
            .setAlias(KEY_ALIAS)
            .setSubject(X500Principal(("CN=$KEY_ALIAS")))
            .setSerialNumber(BigInteger.TEN)
            .setStartDate(start.time)
            .setEndDate(end.time)
            .build()

        KeyPairGenerator.getInstance("RSA", KEY_STORE).also { generator ->
            generator.initialize(spec)
            generator.genKeyPair()
        }
    }

    private fun rsaEncrypt(secret: ByteArray): ByteArray {
        val entry = keystore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        val cipher = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL")
        cipher.init(Cipher.ENCRYPT_MODE, entry.certificate.publicKey)

        return ByteArrayOutputStream().use { baos ->
            CipherOutputStream(baos, cipher).use {
                it.write(secret)
            }
            baos.toByteArray()
        }
    }

    private fun rsaDecrypt(encrypted: ByteArray): ByteArray {
        val entry = keystore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        val output = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL")
        output.init(Cipher.DECRYPT_MODE, entry.privateKey)

        val values = arrayListOf<Byte>()
        ByteArrayInputStream(encrypted).use {  bais ->
            CipherInputStream(bais, output).use {
                var nextByte = it.read()
                while (nextByte != -1) {
                    values.add(nextByte.toByte())
                    nextByte = it.read()
                }
            }
        }

        val bytes = ByteArray(values.size)
        values.forEachIndexed { index, byte ->
            bytes[index] = byte
        }
        return bytes
    }

    private fun generateSecureRandom(): ByteArray {
        val key = ByteArray(16)
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(key)
        return key
    }

    companion object {
        private const val KEY_ALIAS = "behealthy_rsa_key_alias"
        private const val SHARED_PREFERENCE_NAME = "behealthy_encrypted_shared_preferences_api22"
        private const val ENCRYPTED_KEY = "behealthy_rsa_encrypted_key"
        private const val KEY_STORE = "AndroidKeyStore"
        private const val RSA_MODE = "RSA/ECB/PKCS1Padding"
        private const val AES_MODE = "AES/CBC/PKCS5PADDING"
        private const val TEST_KEY = "test_key"
        const val IV_KEY = "recovery_iv_key"
    }
}
