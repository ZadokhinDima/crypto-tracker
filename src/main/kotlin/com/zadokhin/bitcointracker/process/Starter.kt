package com.zadokhin.bitcointracker.process

import com.zadokhin.bitcointracker.BinanceClient
import com.zadokhin.bitcointracker.ProcessService
import com.zadokhin.bitcointracker.TelegramClient
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class Starter(val binanceClient: BinanceClient, val processService: ProcessService, val telegramClient: TelegramClient) {

    @PostConstruct
    fun start() {
        processService.createProcess(MainProcess(binanceClient, telegramClient, processService))
    }
}
