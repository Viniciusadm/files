package com.archieapps.fles.data.repository

import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PairingCryptoTest {
    private val desktopRaw = hex("04e71ab56bfa2a4cb246264724c5b860db87bcbb6984d53be890d3d9d108df69346e5d0a5a9065a6823528fce913763d0a49449e23e5d0817e98c4a16b86e0e356")
    private val phonePkcs8 = hex("308187020100301306072a8648ce3d020106082a8648ce3d030107046d306b0201010420020c18136a6a3a10a9270b4984bfca4f657dcd3169b3b24b4d219885f232b849a14403420004a965d90ae3f8508d26d2105acef4f289b907c2036a5ba67a92aa368302ed8bc21a4e8ae5d59c450ee562d37b2c1069b48d27df2b4086eaccccc72ec2ccbe95a4")
    private val webCryptoShared = hex("3e163afc1677cc18f4bc1ed502205c2f74b0e7f506e8babebf11df0d53d03a62")

    @Test
    fun codeMatchesWebClient() {
        assertEquals("D4JAA-TWZHC", PairingCrypto.code(desktopRaw))
        assertEquals("5YKYX-EG9QB", PairingCrypto.code(ByteArray(65) { 7 }))
    }

    @Test
    fun normalizesTypedCodes() {
        assertEquals("D4JAA-TWZHC", PairingCrypto.normalizeCode("d4jaa twzhc"))
        assertEquals("AB102-113X9", PairingCrypto.normalizeCode("ab1o2-il3x9"))
    }

    @Test
    fun sharedKeyMatchesWebCrypto() {
        val privateKey = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(phonePkcs8))
        val params = (PairingCrypto.generateKeyPair().public as ECPublicKey).params
        assertArrayEquals(webCryptoShared, PairingCrypto.sharedKey(privateKey, desktopRaw, params))
    }

    @Test
    fun encodesPointsRoundTrip() {
        val publicKey = PairingCrypto.generateKeyPair().public as ECPublicKey
        val raw = PairingCrypto.encodePoint(publicKey)
        assertEquals(65, raw.size)
        assertEquals(publicKey.w, PairingCrypto.decodePoint(raw, publicKey.params).w)
    }

    private fun hex(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
