package com.zadokhin.bitcointracker

import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@EnableScheduling
@Component
class FeaturesScheduling(val strategiesService: StrategiesService) {

    val currencies = listOf(
        "BTCUSDT",
    )

    //@Scheduled(cron = "0/10 * * * * *")
    fun buysPercentage() {
        val limit = 100
        currencies.forEach { strategiesService.buysPercentage(it, limit) }
    }

    fun volumeIncrease() {
        val limit = 3
        val batchSize = 100
        currencies.forEach { strategiesService.volumeIncrease(it, limit, batchSize) }
    }
}