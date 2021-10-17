package com.zadokhin.bitcointracker

import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Service
class BinanceClient(val restTemplate: RestTemplate) {

    fun getLastTrades(currency: String, limit: Int): Array<BinanceTrade> {
        val fullUrl = "https://api.binance.com/api/v3/trades?symbol=$currency&limit=$limit"
        try {
            val response = restTemplate.getForEntity(fullUrl, Array<BinanceTrade>::class.java)
            return if (response.body != null) response.body!! else arrayOf()
        } catch (exception: HttpClientErrorException) {
            println("ERROR: $currency")
            throw exception
        }
    }

    fun getLastBitcoinVolumes(currency: String, batchSize: Int): List<Double> {
        val fullUrl = "https://api.binance.com/api/v3/klines?symbol=$currency&limit=$batchSize&interval=5m"
        try {
            val response = restTemplate.getForEntity(fullUrl, Array<out Array<out Any>>::class.java)
            return if (response.body != null) {
                return response.body!!.map { it[5].toString().toDouble() }
            } else listOf()
        } catch (exception: HttpClientErrorException) {
            println("ERROR: $currency")
            throw exception
        }
    }
}

data class BinanceTrade(
    val id: Long,
    val price: Double,
    val qty: Double,
    val quoteQty: Double,
    val time: Long,
    val isBuyerMaker: Boolean,
    val isBestMatch: Boolean
)