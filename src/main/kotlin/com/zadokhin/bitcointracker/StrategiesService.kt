package com.zadokhin.bitcointracker

import com.zadokhin.bitcointracker.strategies.BBStrategy
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Service

@Service
@EnableScheduling
class StrategiesService(
    val binanceClient: BinanceClient,
    val bbStrategy: BBStrategy, val telegramClient: TelegramClient
) {

    fun startBB(currency: String) {
        bbStrategy.start(currency)
    }

    fun updateBB() {
        bbStrategy.process()
    }

    fun stopBB() {
        bbStrategy.finish()
    }

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
