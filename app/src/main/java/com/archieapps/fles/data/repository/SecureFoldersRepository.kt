package com.archieapps.fles.data.repository

import com.archieapps.fles.data.local.SecureFolderStorage
import com.archieapps.fles.data.remote.ApiService
import com.archieapps.fles.data.remote.SecureFolderMetaResponse
import com.archieapps.fles.data.remote.SecurePinSetupRequest
import com.archieapps.fles.data.remote.SecurePinVerifyRequest
import com.archieapps.fles.data.remote.requireBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class SecureFoldersRepository(
    private val apiService: ApiService,
    private val cryptoRepository: CryptoRepository,
    private val attachmentIo: AttachmentIo,
    private val storage: SecureFolderStorage
) {
    private val random = SecureRandom()
    private val _state = MutableStateFlow(SecureFolderSessionState())
    val state: StateFlow<SecureFolderSessionState> = _state

    suspend fun refreshMeta(): SecureFolderMetaResponse? {
        val local = storage.getMeta()
        if (local != null) {
            _state.value = _state.value.copy(configured = true)
        }
        return runCatching { apiService.secureMeta().requireBody() }
            .onSuccess { meta ->
                if (meta.configured) storage.saveMeta(meta)
                _state.value = _state.value.copy(configured = meta.configured)
            }
            .getOrDefault(local)
    }

    suspend fun setup(pin: String): SecureFolderMetaResponse {
        require(pin.matches(Regex("^[0-9]{4}$"))) { "Informe um PIN de 4 dígitos." }
        val request = buildSetupRequest(pin)
        val meta = apiService.setupSecurePin(request).requireBody()
        storage.saveMeta(meta)
        unlockWithMeta(pin, meta)
        return meta
    }

    suspend fun unlock(pin: String) {
        require(pin.matches(Regex("^[0-9]{4}$"))) { "Informe um PIN de 4 dígitos." }
        val meta = storage.getMeta()
            ?: runCatching { apiService.verifySecurePin(SecurePinVerifyRequest(pin)).requireBody() }.getOrElse { throw IllegalStateException("Configuração segura indisponível offline.") }
        storage.saveMeta(meta)
        unlockWithMeta(pin, meta)
        runCatching { apiService.verifySecurePin(SecurePinVerifyRequest(pin)).requireBody() }
    }

    suspend fun reset(pin: String): SecureFolderMetaResponse {
        require(pin.matches(Regex("^[0-9]{4}$"))) { "Informe um PIN de 4 dígitos." }
        val request = buildSetupRequest(pin)
        val meta = apiService.resetSecurePin(request).requireBody()
        storage.clear()
        storage.saveMeta(meta)
        unlockWithMeta(pin, meta)
        return meta
    }

    fun lock() {
        attachmentIo.clearTemporaryCache()
        _state.value = _state.value.copy(unlocked = false, key = null)
    }

    fun secureKey(): ByteArray? = _state.value.key

    fun encryptString(value: String): String {
        val key = secureKey() ?: throw IllegalStateException("Pastas seguras bloqueadas.")
        return cryptoRepository.encryptStringWithKey(value, key, 2)
    }

    fun tryDecryptString(value: String?): String {
        val text = value.orEmpty()
        if (text.isBlank()) return text
        if (!cryptoRepository.looksLikeEnvelope(text)) return text
        val key = secureKey() ?: return ""
        return runCatching { cryptoRepository.decryptStringWithKey(text, key) }.getOrElse { "" }
    }

    fun tryDecryptStringOrNull(value: String?): String? {
        if (value == null) return null
        if (value.isBlank()) return value
        if (!cryptoRepository.looksLikeEnvelope(value)) return value
        val key = secureKey() ?: return null
        return runCatching { cryptoRepository.decryptStringWithKey(value, key) }.getOrNull()
    }

    private suspend fun buildSetupRequest(pin: String): SecurePinSetupRequest {
        val secureKey = ByteArray(32).also(random::nextBytes)
        val salt = ByteArray(16).also(random::nextBytes)
        val iterations = 200_000
        val wrappingKey = deriveWrappingKey(pin, salt, iterations)
        return SecurePinSetupRequest(
            pin = pin,
            keySalt = cryptoRepository.encodeBytes(salt),
            keyIterations = iterations,
            keyEnvelope = cryptoRepository.encryptEnvelope(secureKey, wrappingKey, 1),
            keyVersion = 1
        )
    }

    private suspend fun unlockWithMeta(pin: String, meta: SecureFolderMetaResponse) {
        val salt = cryptoRepository.decodeBytes(meta.keySalt ?: throw IllegalStateException("Configuração segura incompleta."))
        val iterations = meta.keyIterations ?: throw IllegalStateException("Configuração segura incompleta.")
        val envelope = meta.keyEnvelope ?: throw IllegalStateException("Configuração segura incompleta.")
        val wrappingKey = deriveWrappingKey(pin, salt, iterations)
        val secureKey = cryptoRepository.decryptEnvelope(envelope, wrappingKey)
        _state.value = SecureFolderSessionState(configured = true, unlocked = true, key = secureKey)
    }

    private suspend fun deriveWrappingKey(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val globalKey = cryptoRepository.getKey() ?: throw IllegalStateException("Chave E2EE não configurada.")
        val material = "$globalKey:$pin".toCharArray()
        val spec = PBEKeySpec(material, salt, iterations, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }
}

data class SecureFolderSessionState(
    val configured: Boolean = false,
    val unlocked: Boolean = false,
    val key: ByteArray? = null
)
