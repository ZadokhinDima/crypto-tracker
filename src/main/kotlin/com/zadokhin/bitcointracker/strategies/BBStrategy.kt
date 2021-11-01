package com.zadokhin.bitcointracker.strategies

import com.zadokhin.bitcointracker.BinanceClient
import com.zadokhin.bitcointracker.OrderResponse
import com.zadokhin.bitcointracker.TelegramClient
import com.zadokhin.bitcointracker.format
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BBStrategy(val binanceClient: BinanceClient, val telegramClient: TelegramClient) {

    private var instance: StrategyInstance? = null
    private val qty = 0.001

    @Synchronized
    fun start(currency: String) {
        if (instance == null) {
            val firstOrder = binanceClient.createBuyOrderMarketPrice(currency, qty)
            if (firstOrder.status == "FILLED") {
                println("CREATED BUY ORDER: $firstOrder")
                telegramClient.sendNotification("Зайшов за ${firstOrder.cummulativeQuoteQty} $.")
                instance = StrategyInstance(firstOrder)
            }
        }
    }

    @Synchronized
    fun process() {
        if (instance != null) {
            val currency = instance!!.currency
            val currentPrice = binanceClient.getPrice(currency)
            if (currentPrice < instance!!.lastBuyPrice * 0.997) {
                val additionalOrder = binanceClient.createBuyOrderMarketPrice(currency, qty)
                telegramClient.sendNotification("Докупляю за ${additionalOrder.cummulativeQuoteQty}.")
                println("CREATED BUY ORDER: $additionalOrder")
                if (additionalOrder.status == "FILLED") {
                    instance!!.addOrder(additionalOrder)
                }
            }
        }
    }

    @Synchronized
    fun finish() {
        if (instance != null) {
            do {
                val sellOrder = binanceClient.createSellOrderMarketPrice(instance!!.currency, instance!!.qty)
                println("CREATED SELL ORDER: $sellOrder")
                if (sellOrder.status == "FILLED") {
                    telegramClient.sendNotification("Продав за ${sellOrder.cummulativeQuoteQty}.")
                    calculateProfit(instance!!.buys, sellOrder)
                    instance = null
                }
            } while (sellOrder.status == "FILLED")
        }
    }

    private fun calculateProfit(buys: List<OrderResponse>, sell: OrderResponse) {
        val spent = buys.sumOf { it.cummulativeQuoteQty }
        val received = sell.cummulativeQuoteQty
        val emoji = if (received > spent) "\uD83D\uDFE2" else "\uD83D\uDFE2"
        telegramClient.sendNotification("Профіт: ${(received - spent).format(2)} $ $emoji")
    }

}

class StrategyInstance(order: OrderResponse) {

    val currency: String
    var lastBuyPrice: Double
    var qty: Double
    var buys: List<OrderResponse>
    var sellTime: LocalDateTime? = null

    init {
        currency = order.symbol
        qty = order.executedQty
        lastBuyPrice = order.price
        buys = listOf(order)
    }

    fun addOrder(order: OrderResponse) {
        buys = buys + order
        lastBuyPrice = order.cummulativeQuoteQty / order.executedQty
        qty += order.executedQty
    }
}