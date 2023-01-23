package com.example.behealthy.shared.security

import android.content.Context
import android.os.Build
import kotlin.reflect.KClass

class SecurePreferences(context: Context) {

    private val storage: SecureStorage =
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            Encryption(context)
        } else {
            Encryption22(context)
        }

    internal inline fun <reified T : Any> get(key: String): T? = try {
        storage.get(key, T::class) as T?
    } catch (e: java.lang.Exception) { null }

    fun put(key: String, value: Any) = storage.put(key, value)
    fun delete(key: String) = storage.delete(key)
    fun contains(key: String) = storage.contains(key)
}

interface SecureStorage {
    fun put(key: String, value: Any)
    fun get(key: String, clazz: KClass<*>): Any?
    fun delete(key: String)
    fun contains(key: String): Boolean
}
