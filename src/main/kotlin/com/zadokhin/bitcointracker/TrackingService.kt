package com.zadokhin.bitcointracker

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
@EnableScheduling
class TrackingService(val restTemplate: RestTemplate) {

    val limit = 3

    @Value("\${bot.token}")
    val botToken: String = ""

    val batchSize = 10

    val currencies = listOf(
        "BTCUSDT",
    )

    @Scheduled(cron = "0/10 * * * * *")
    fun coinVolumeTracker() {
        currencies.forEach { searchRepeatedTrades(it) }
    }

    fun searchRepeatedTrades(currency: String) {
        val lastTrades = getLastTrades(currency)
        val upTrades = lastTrades.filter { it.isBuyerMaker }
        val downTrades = lastTrades.filter { !it.isBuyerMaker }
        val sizeLimit = 3
        val message: String = upTrades
            .groupBy { it.qty }
            .filter { it.value.size > sizeLimit }
            .map { "$currency | ${it.key.format(4)} | ${it.value.size} | \uD83D\uDFE2" }
            .joinToString(separator = "\n") +
                "\n" +
                downTrades
                    .groupBy { it.qty }
                    .filter { it.value.size > sizeLimit }
                    .map { "$currency | ${it.key.format(4)} | ${it.value.size} | \uD83D\uDD34" }
                    .joinToString(separator = "\n")

        sendNotification(message)
    }

    fun processCurrency(currency: String) {
        val lastBitcoinVolumes = getLastBitcoinVolumes(currency)
        val volumeScore = lastBitcoinVolumes.last() * batchSize / lastBitcoinVolumes.take(batchSize - 1).sum()

        println("CURRENCY: $currency")
        println("CANDLE VOLUMES: $lastBitcoinVolumes")
        println("SCORE: $volumeScore")
        println()

        if (volumeScore > limit)
            sendNotification("$currency -> $volumeScore")
    }

    fun sendNotification(text: String) {
        val telegramBotApiUrl = "https://api.telegram.org"
        val method = "sendMessage"
        val chatId = -1001477278594L
        val fullUrl = "$telegramBotApiUrl/bot$botToken/$method"
        restTemplate.postForEntity(fullUrl, SendMessageRequest(chatId = chatId, text = text), Any::class.java)
    }

    fun getLastBitcoinVolumes(currency: String): List<Double> {
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

    fun getLastTrades(currency: String): Array<BinanceTrade> {
        val limit = 500
        val fullUrl = "https://api.binance.com/api/v3/trades?symbol=$currency&limit=$limit"
        try {
            val response = restTemplate.getForEntity(fullUrl, Array<BinanceTrade>::class.java)
            return if (response.body != null) response.body!! else arrayOf()
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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SendMessageRequest(
    @JsonProperty("chat_id") val chatId: Long,
    val text: String,
    @JsonProperty("reply_markup") val replyMarkup: KeyboardMarkup? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class KeyboardMarkup(
    @JsonProperty("inline_keyboard")
    val keyboard: List<List<KeyboardButton>>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class KeyboardButton(
    val text: String,
    @JsonProperty("callback_data")
    val callback: String
)

fun Double.format(digits: Int) = "%.${digits}f".format(this)
