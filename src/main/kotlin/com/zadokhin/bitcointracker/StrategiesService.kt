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
class StrategiesService(val binanceClient: BinanceClient, val telegramClient: TelegramClient) {

    fun buysPercentage(currency: String, limit: Int) {
        val lastTrades = binanceClient.getLastTrades(currency, limit)
        val buyQuantity = lastTrades.filter { it.isBuyerMaker }.sumOf { it.qty }
        val overallQuantity = lastTrades.sumOf { it.qty }
        telegramClient.sendNotification("BUY: ${(buyQuantity / overallQuantity * limit).format(2)}%")
    }

    fun volumeIncrease(currency: String, batchSize: Int, limit: Int) {
        val lastBitcoinVolumes = binanceClient.getLastBitcoinVolumes(currency, batchSize)
        val volumeScore = lastBitcoinVolumes.last() * batchSize / lastBitcoinVolumes.take(batchSize - 1).sum()

        println("CURRENCY: $currency")
        println("CANDLE VOLUMES: $lastBitcoinVolumes")
        println("SCORE: $volumeScore")
        println()

        if (volumeScore > limit)
            telegramClient.sendNotification("$currency -> $volumeScore")
    }

}



fun Double.format(digits: Int) = "%.${digits}f".format(this)
