package com.zadokhin.bitcointracker.process

import com.zadokhin.bitcointracker.BinanceClient
import com.zadokhin.bitcointracker.ProcessService
import com.zadokhin.bitcointracker.TelegramClient
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

interface Process {
    fun start()
    fun update()
    fun completed(): Boolean
    fun getPrice(): Double
}

class MainProcess (private val binanceClient: BinanceClient,
                   private val telegramClient: TelegramClient,
                   private val processService: ProcessService): Process {

    val currency: String = "BTCUSDT"
    var lastPrice: Double = binanceClient.getPrice(currency)
    val threshold = 750
    var childProcess: Process? = null

    override fun start() {
        childProcess = BuyProcess(binanceClient, telegramClient)
        processService.createProcess(childProcess!!)
    }

    @Synchronized
    override fun update() {
        if (childProcess == null) {
            val price = binanceClient.getPrice(currency)
            if (price > lastPrice + threshold) {
                sell()
            } else if (price < lastPrice - threshold) {
                buy()
            }
            return
        }
        val child = childProcess!!
        if (child.completed()) {
            lastPrice = child.getPrice()
            childProcess = null
        }
    }

    private fun sell() {
        childProcess = SellProcess(binanceClient, telegramClient)
        processService.createProcess(childProcess!!)
    }

    private fun buy() {
        childProcess = BuyProcess(binanceClient, telegramClient)
        processService.createProcess(childProcess!!)
    }

    override fun completed(): Boolean {
        return false
    }

    override fun getPrice(): Double {
        return lastPrice
    }


}


class BuyProcess (private val binanceClient: BinanceClient, private val telegramClient: TelegramClient): Process {
    var completed = false
    val currency: String = "BTCUSDT"
    var orderId: Long? = null
    var stopPrice: Double? = null
    val threshold = 100


    override fun start() {
        val currentPrice = binanceClient.getPrice(currency)
        stopPrice = currentPrice + threshold
        val order = binanceClient.createBuyOrder(currency, stopPrice!!, qty = 0.01)
        orderId = order.orderId
        telegramClient.sendNotification("Ціна $currentPrice, Стоп лосс на ${order.price}$")
    }

    override fun update() {
        if (completed)
            return

        val currentPrice = binanceClient.getPrice(currency)
        val order = binanceClient.getOrder(currency, orderId!!)
        if (currentPrice < stopPrice!! - (2 * threshold) && order.status == "NEW") {
            stopPrice = currentPrice + threshold
            val updatedOrder = binanceClient.createBuyOrder(currency, stopPrice!!, qty = 0.01)
            binanceClient.deleteOrder(currency, orderId!!)
            orderId = updatedOrder.orderId
            telegramClient.sendNotification("Ціна $currentPrice, Стоп лосс на ${updatedOrder.price}$")
        }
        if (order.status == "FILLED") {
            telegramClient.sendNotification("Купив за ${order.price}$")
            completed = true
        }
    }

    override fun completed(): Boolean {
        return completed
    }

    override fun getPrice(): Double {
        return stopPrice!!
    }

}

class SellProcess (private val binanceClient: BinanceClient, private val telegramClient: TelegramClient): Process {
    var completed = false
    val currency: String = "BTCUSDT"
    var orderId: Long? = null
    var stopPrice: Double? = null
    val threshold = 100


    override fun start() {
        val currentPrice = binanceClient.getPrice(currency)
        stopPrice = currentPrice - threshold
        val order = binanceClient.createSellOrder(currency, stopPrice!!, qty = 0.01)
        orderId = order.orderId
        telegramClient.sendNotification("Ціна $currentPrice, Стоп лосс на ${order.price}$")
    }

    override fun update() {
        if (completed)
            return

        val currentPrice = binanceClient.getPrice(currency)
        val order = binanceClient.getOrder(currency, orderId!!)
        if (currentPrice > stopPrice!! + (2 * threshold) && order.status == "NEW") {
            stopPrice = currentPrice - threshold
            val updatedOrder = binanceClient.createSellOrder(currency, stopPrice!!, qty = 0.01)
            binanceClient.deleteOrder(currency, orderId!!)
            orderId = updatedOrder.orderId
            telegramClient.sendNotification("Ціна $currentPrice, Стоп лосс на ${updatedOrder.price}$")
        }
        if (order.status == "FILLED") {
            telegramClient.sendNotification("Продав за ${order.price}$")
            completed = true
        }
    }

    override fun completed(): Boolean {
        return completed
    }

    override fun getPrice(): Double {
        return stopPrice!!
    }

}


data class Order(val operation: Operation, val price: Double)

enum class Operation {
    BUY, SELL
}
