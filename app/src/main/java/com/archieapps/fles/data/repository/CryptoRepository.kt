package com.archieapps.fles.data.repository

import android.util.Base64
import com.archieapps.fles.data.local.KeyStorage
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoRepository(private val keyStorage: KeyStorage) {
    private val random = SecureRandom()

    suspend fun hasKey(): Boolean {
        return keyStorage.getKey() != null
    }

    suspend fun getKey(): String? {
        return keyStorage.getKey()
    }

    suspend fun saveKey(value: String) {
        val trimmed = value.trim()
        decodeKey(trimmed)
        keyStorage.setKey(trimmed)
    }

    suspend fun clearKey() {
        keyStorage.setKey(null)
    }

    fun generateKey(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    suspend fun fingerprint(): String? {
        val key = keyStorage.getKey() ?: return null
        val bytes = decodeKey(key)
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
        return hash.take(8).joinToString(":") { "%02X".format(it) }
    }

    suspend fun encryptString(value: String): String {
        if (value.isBlank()) return value
        val key = decodeKey(keyStorage.getKey() ?: throw IllegalStateException("Chave E2EE não configurada."))
        val iv = ByteArray(12)
        random.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return "{\"version\":1,\"algorithm\":\"AES-256-GCM\",\"iv\":\"${encode(iv)}\",\"ciphertext\":\"${encode(ciphertext)}\"}"
    }

    suspend fun tryDecryptString(value: String?): String {
        val text = value.orEmpty()
        if (text.isBlank()) return text
        return runCatching { decryptString(text) }.getOrElse { text }
    }

    private suspend fun decryptString(value: String): String {
        val key = decodeKey(keyStorage.getKey() ?: throw IllegalStateException("Chave E2EE não configurada."))
        val iv = extractJsonValue(value, "iv") ?: throw IllegalArgumentException("Envelope E2EE inválido")
        val ciphertext = extractJsonValue(value, "ciphertext") ?: throw IllegalArgumentException("Envelope E2EE inválido")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, decode(iv)))
        return cipher.doFinal(decode(ciphertext)).toString(Charsets.UTF_8)
    }

    private fun decodeKey(value: String): ByteArray {
        val bytes = decode(value)
        require(bytes.size == 32) { "A chave precisa ter 32 bytes (AES-256) em base64." }
        return bytes
    }

    private fun encode(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun decode(value: String): ByteArray {
        return Base64.decode(value, Base64.DEFAULT)
    }

    private fun extractJsonValue(json: String, key: String): String? {
        return Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.getOrNull(1)
    }
}
