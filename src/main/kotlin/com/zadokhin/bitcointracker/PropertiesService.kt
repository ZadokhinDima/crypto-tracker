package com.zadokhin.bitcointracker

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class PropertiesHolder {

    @Value("\${process.main.threshold}")
    val mainThreshold = 750
    @Value("\${process.buy.threshold}")
    val buyThreshold = 100
    @Value("\${process.sell.threshold}")
    val sellThreshold = 100
    @Value("\${process.trade.size}")
    val tradeSize = 0.01
    @Value("\${binance.api.host}")
    val binanceHost: String = ""

}
