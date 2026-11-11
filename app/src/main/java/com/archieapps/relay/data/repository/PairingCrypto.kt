package com.archieapps.relay.data.repository

import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object PairingCrypto {
    private const val CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val COORDINATE_BYTES = 32

    data class Sealed(val publicKey: ByteArray, val iv: ByteArray, val ciphertext: ByteArray)

    fun code(publicKey: ByteArray): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(publicKey)
        val builder = StringBuilder()
        var value = 0
        var bits = 0
        for (byte in hash) {
            value = (value shl 8) or (byte.toInt() and 0xFF)
            bits += 8
            while (bits >= 5 && builder.length < 10) {
                builder.append(CROCKFORD[(value ushr (bits - 5)) and 31])
                bits -= 5
            }
            value = value and ((1 shl bits) - 1)
            if (builder.length >= 10) break
        }
        return "${builder.substring(0, 5)}-${builder.substring(5)}"
    }

    fun normalizeCode(value: String): String {
        val clean = value.uppercase()
            .filter { it.isLetterOrDigit() }
            .replace('O', '0')
            .replace('I', '1')
            .replace('L', '1')
        return if (clean.length == 10) "${clean.substring(0, 5)}-${clean.substring(5)}" else clean
    }

    fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        return generator.generateKeyPair()
    }

    fun encodePoint(publicKey: ECPublicKey): ByteArray {
        return byteArrayOf(0x04) + unsigned(publicKey.w.affineX) + unsigned(publicKey.w.affineY)
    }

    fun decodePoint(raw: ByteArray, params: ECParameterSpec): ECPublicKey {
        require(raw.size == 1 + COORDINATE_BYTES * 2 && raw[0] == 0x04.toByte()) { "Chave pública do computador inválida." }
        val x = BigInteger(1, raw.copyOfRange(1, 1 + COORDINATE_BYTES))
        val y = BigInteger(1, raw.copyOfRange(1 + COORDINATE_BYTES, raw.size))
        return KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(ECPoint(x, y), params)) as ECPublicKey
    }

    fun sharedKey(privateKey: PrivateKey, peerRaw: ByteArray, params: ECParameterSpec): ByteArray {
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(decodePoint(peerRaw, params), true)
        return MessageDigest.getInstance("SHA-256").digest(agreement.generateSecret())
    }

    fun seal(plain: ByteArray, peerRaw: ByteArray): Sealed {
        val pair = generateKeyPair()
        val publicKey = pair.public as ECPublicKey
        val key = sharedKey(pair.private, peerRaw, publicKey.params)
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return Sealed(encodePoint(publicKey), iv, cipher.doFinal(plain))
    }

    private fun unsigned(value: BigInteger): ByteArray {
        val bytes = value.toByteArray()
        return when {
            bytes.size == COORDINATE_BYTES -> bytes
            bytes.size > COORDINATE_BYTES -> bytes.copyOfRange(bytes.size - COORDINATE_BYTES, bytes.size)
            else -> ByteArray(COORDINATE_BYTES - bytes.size) + bytes
        }
    }
}
