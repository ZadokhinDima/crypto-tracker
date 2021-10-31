package com.zadokhin.bitcointracker

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils

@Component
class CryptoService {

    @Value("\${binance.secret.key}")
    val secretKey: String = ""

    fun getHmacSignature(queryString: String): String {
        val keyBytes = secretKey.toByteArray()
        val hm256 = HmacUtils(HmacAlgorithms.HMAC_SHA_256, keyBytes)
        return hm256.hmacHex(queryString)
    }
}