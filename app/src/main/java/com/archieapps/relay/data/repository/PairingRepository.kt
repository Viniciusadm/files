package com.archieapps.relay.data.repository

import com.archieapps.relay.data.remote.ApiService
import com.archieapps.relay.data.remote.ApprovePairingRequest
import com.archieapps.relay.data.remote.requireBody
import retrofit2.HttpException

class PairingRepository(
    private val apiService: ApiService,
    private val cryptoRepository: CryptoRepository
) {
    suspend fun approve(typedCode: String): String? {
        val key = cryptoRepository.getKey() ?: throw IllegalStateException("Configure a chave de criptografia antes de conectar um computador.")
        val pending = try {
            apiService.pendingPairing().requireBody()
        } catch (error: HttpException) {
            if (error.code() == 404) throw IllegalStateException("Nenhum computador aguardando. Abra o Relay no computador e gere um código.")
            throw error
        }
        val desktopKey = cryptoRepository.decodeBytes(pending.publicKey)
        if (PairingCrypto.code(desktopKey) != PairingCrypto.normalizeCode(typedCode)) {
            throw IllegalArgumentException("Código incorreto. Confira o código mostrado no computador.")
        }
        val sealed = PairingCrypto.seal(key.toByteArray(Charsets.UTF_8), desktopKey)
        apiService.approvePairing(
            pending.id,
            ApprovePairingRequest(
                publicKey = cryptoRepository.encodeBytes(sealed.publicKey),
                iv = cryptoRepository.encodeBytes(sealed.iv),
                ciphertext = cryptoRepository.encodeBytes(sealed.ciphertext)
            )
        ).requireBody()
        return pending.deviceName
    }
}
