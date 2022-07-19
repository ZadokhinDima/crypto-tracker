package com.zadokhin.bitcointracker.process

import com.zadokhin.bitcointracker.BinanceClient
import com.zadokhin.bitcointracker.ProcessService
import com.zadokhin.bitcointracker.PropertiesHolder
import com.zadokhin.bitcointracker.TelegramClient
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class Starter(val binanceClient: BinanceClient, val processService: ProcessService, val telegramClient: TelegramClient, val propertiesHolder: PropertiesHolder) {

    @PostConstruct
    fun start() {
        notifyAboutStart()
        processService.createProcess(MainProcess(binanceClient, telegramClient, processService, propertiesHolder))
    }

    private fun notifyAboutStart() {
        val startMessage =
            """
                Bot started. 
                Properties:
                Binance host: ${propertiesHolder.binanceHost}
                Currency: ${propertiesHolder.currency}
                Trade size: ${propertiesHolder.tradeSize}
                Main threshold: ${propertiesHolder.mainThreshold}
                Buy threshold: ${propertiesHolder.buyThreshold}
        """.trimIndent()
        telegramClient.sendNotification(startMessage)
    }
}
