package com.zadokhin.bitcointracker

import com.zadokhin.bitcointracker.process.Process
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@EnableScheduling
@Component
class FeaturesScheduling(
    val strategiesService: StrategiesService,
    val processService: ProcessService
) {

    val currencies = listOf(
        "BTCUSDT",
    )

    var trackedProcesses: List<Process> = listOf()

    // @Scheduled(cron = "0/30 * * * * *")
    fun bbStrategy() {
        strategiesService.updateBB()
    }

    @Scheduled(cron = "0/30 * * * * *")
    fun processUpdate() {
        processService.updateTrackedProcesses()
    }

    @Scheduled(cron = "0 0/10 * * * *")
    fun notifyAlive() {
        processService.checkAlive()
    }

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
