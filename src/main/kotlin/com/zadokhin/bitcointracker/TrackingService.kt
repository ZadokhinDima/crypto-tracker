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
        "SOLUSDT",
        "ETHUSDT",
        "FTMUSDT",
        "ADAUSDT",
        "DOTUSDT",
        "LINKUSDT",
        "XRPUSDT",
        "BNBUSDT",
        "AVAXUSDT",
        "TRXUSDT",
        "FILUSDT",
        "XTZUSDT",
        "ATOMUSDT",
        "HBARUSDT",
        "LTCUSDT",
        "CRVUSDT",
        "RENUSDT",
        "CELRUSDT",
        "MATICUSDT",
        "LUNAUSDT",
        "AAVEUSDT",
        "ALGOUSDT",
        "EOSUSDT",
        "DOGEUSDT",
        "SRMUSDT",
        "SUSHIUSDT",
        "ETCUSDT",
        "THETAUSDT",
        "AXSUSDT",
        "UNIUSDT",
        "NEARUSDT",
        "IOSTUSDT",
        "ICPUSDT",
        "EGLDUSDT",
        "SNXUSDT",
        "C98USDT",
        "BCHUSDT",
        "ALICEUSDT",
        "ICXUSDT",
        "ONEUSDT",
        "SXPUSDT",
        "DYDXUSDT",
        "KEEPUSDT",
        "ETHUSDT",
        "VETUSDT",
        "RUNEUSDT",
        "KSMUSDT",
        "IOTAUSDT",
        "RAYUSDT",
        "COMPUSDT",
        "XLMUSDT",
        "SKLUSDT",
        "1INCHUSDT",
        "CHZUSDT",
        "ATAUSDT",
        "ALPHAUSDT",
        "OMGUSDT",
        "TLMUSDT",
        "BAKEUSDT",
        "NEOUSDT",
        "YFIUSDT",
        "SANDUSDT",
        "MASKUSDT",
        "GRTUSDT",
        "LRCUSDT",
        "BANDUSDT",
        "BELUSDT",
        "DODOUSDT",
        "BZRXUSDT",
        "RLCUSDT",
        "XMRUSDT",
        "COTIUSDT",
        "WAVESUSDT",
        "TRBUSDT",
        "DENTUSDT",
        "QTUMUSDT",
        "ZECUSDT",
        "BTTUSDT",
        "OCEANUSDT",
        "LITUSDT",
        "KAVAUSDT",
        "SFPUSDT",
        "CHRUSDT",
        "BALUSDT",
        "FLMUSDT",
        "MTLUSDT",
        "TOMOUSDT",
        "LINAUSDT",
        "DASHUSDT",
        "YFIIUSDT",
        "ENJUSDT",
        "CVCUSDT",
        "REEFUSDT",
        "BTSUSDT",
        "ZILUSDT",
        "HOTUSDT",
        "UNFIUSDT",
        "ANKRUSDT",
        "MKRUSDT",
        "NKNUSDT",
        "OGNUSDT",
        "CTKUSDT",
        "BLZUSDT",
        "ONTUSDT",
        "AUDIOUSDT",
        "RSRUSDT",
        "IOTXUSDT",
        "ZENUSDT",
        "AKROUSDT",
        "GTCUSDT",
        "KNCUSDT",
        "MANAUSDT",
        "ZRXUSDT",
        "RVNUSDT",
        "STORJUSDT",
        "HNTUSDT",
        "BATUSDT",
        "XEMUSDT",
        "STMXUSDT",
        "DGBUSDT",
        "SCUSDT",
    )

    @Scheduled(cron = "55 * * * * *")
    fun coinVolumeTracker() {
        currencies.forEach { processCurrency(it) }
    }

    fun processCurrency(currency: String) {
        val lastBitcoinVolumes = getLastBitcoinVolumes(currency)
        val volumeScore = lastBitcoinVolumes.last() * batchSize / lastBitcoinVolumes.take(batchSize - 1).sum()

        println("CURRENCY: $currency")
        println("CANDLE VOLUMES: $lastBitcoinVolumes")
        println("SCORE: $volumeScore")
        println()

        if (volumeScore > limit)
            sendNotification(currency, volumeScore)
    }

    fun sendNotification(currency: String, volumeScore: Double) {
        val telegramBotApiUrl = "https://api.telegram.org"
        val method = "sendMessage"
        val chatId = -1001477278594L
        val fullUrl = "$telegramBotApiUrl/bot$botToken/$method"
        restTemplate.postForEntity(fullUrl, SendMessageRequest(chatId = chatId, text = "$currency -> $volumeScore"), Any::class.java)
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

}

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
