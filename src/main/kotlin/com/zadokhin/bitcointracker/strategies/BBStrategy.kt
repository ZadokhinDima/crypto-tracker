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
            println("CREATED BUY ORDER: $firstOrder")
            telegramClient.sendNotification("Зайшов по ${firstOrder.price}.")
            instance = StrategyInstance(firstOrder)
        }
    }

    @Synchronized
    fun process() {
        if (instance != null) {
            println("process order called...")
            val currency = instance!!.currency
            val currentPrice = binanceClient.getPrice(currency)
            if (currentPrice < instance!!.lastBuyPrice * 0.99) {
                val additionalOrder = binanceClient.createBuyOrderMarketPrice(currency, qty)
                telegramClient.sendNotification("Докупляю по ${additionalOrder.price}.")
                println("CREATED BUY ORDER: $additionalOrder")
                instance!!.addOrder(additionalOrder)
            }
        }
    }

    @Synchronized
    fun finish() {
        if (instance != null) {
            val sellOrder = binanceClient.createSellOrderMarketPrice(instance!!.currency, instance!!.qty)
            println("CREATED SELL ORDER: $sellOrder")
            telegramClient.sendNotification("Продав за ${sellOrder.price}.")
            calculateProfit(instance!!.buys, sellOrder)
        }
    }

    private fun calculateProfit(buys: List<OrderResponse>, sell: OrderResponse) {
        val spent = buys.sumOf { it.executedQty * it.price }
        val received = sell.executedQty * sell.price
        val emoji = if (received > spent) "\uF7E2" else "\uF534"
        telegramClient.sendNotification("Профіт: ${(received-spent).format(2)} $ $emoji")
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
        lastBuyPrice = order.price
        qty += order.executedQty
    }
}