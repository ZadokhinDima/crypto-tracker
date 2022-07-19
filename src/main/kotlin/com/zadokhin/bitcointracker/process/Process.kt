package com.zadokhin.bitcointracker.process

import com.zadokhin.bitcointracker.*
import com.zadokhin.bitcointracker.process.MainProcessStatus.*
import com.zadokhin.bitcointracker.process.ChildProcessStatus.*
import java.lang.IllegalStateException

interface Process {
    fun start()
    fun update()
    fun completed(): Boolean
    fun getPrice(): Double
    fun alive()
}

class MainProcess(
    private val binanceClient: BinanceClient,
    private val telegramClient: TelegramClient,
    private val processService: ProcessService,
    private val propertiesHolder: PropertiesHolder
) : Process {
    private val threshold = propertiesHolder.mainThreshold
    private val currency = propertiesHolder.currency
    var childProcess: Process? = null
    private val state: MainProcessState

    init {
        val price = binanceClient.getPrice(currency)
        state = MainProcessState(price, price, CREATED)
    }

    override fun start() {
        notifyState("START")
    }

    @Synchronized
    override fun update() {
        when (state.status) {
            CREATED, AFTER_BUY, AFTER_SELL -> tryCreateBuyOrSell()
            WAITING_BUY, WAITING_SELL -> waitChecks()
        }
    }

    override fun alive() {
        notifyState("ALIVE")
    }

    private fun tryCreateBuyOrSell() {
        val price = binanceClient.getPrice(currency)
        state.currentPrice = price
        childProcess = tryCreateSell(price) ?: tryCreateBuy(price)
    }

    private fun tryCreateBuy(currentPrice: Double) = if (checkBuyCondition(currentPrice)) buy() else null
    private fun tryCreateSell(currentPrice: Double) = if (checkSellCondition(currentPrice)) sell() else null

    private fun checkBuyCondition(currentPrice: Double): Boolean = currentPrice < state.lastPrice - threshold
    private fun checkSellCondition(currentPrice: Double): Boolean = currentPrice > state.lastPrice + threshold

    private fun waitChecks() {
        state.currentPrice = binanceClient.getPrice(currency)
        val child = childProcess!! //Should be not null cause we are waiting for childProcessToFinish
        if (child.completed()) {
            val newStatus = when (state.status) {
                WAITING_BUY -> AFTER_BUY
                WAITING_SELL -> AFTER_SELL
                else -> throw IllegalStateException("INCORRECT WAIT STATUS: ${state.status}")
            }
            state.status = newStatus
            state.lastPrice = child.getPrice()
            childProcess = null
            notifyState("BUY/SELL FINISHED")
        }
    }

    private fun sell(): SellProcess {
        val sellProcess = SellProcess(binanceClient, telegramClient, propertiesHolder)
        processService.createProcess(sellProcess)
        state.status = WAITING_SELL
        state.lastPrice = sellProcess.getPrice()
        notifyState("SELL")
        return sellProcess
    }

    private fun buy(): BuyProcess {
        val buyProcess = BuyProcess(binanceClient, telegramClient, propertiesHolder)
        processService.createProcess(buyProcess)
        state.status = WAITING_BUY
        state.lastPrice = buyProcess.getPrice()
        notifyState("BUY")
        return buyProcess
    }

    private fun notifyState(event: String) {
        telegramClient.sendNotification("[$event] MAIN PROCESS STATE($state)")
    }

    override fun completed(): Boolean {
        return false
    }

    override fun getPrice(): Double {
        return state.lastPrice
    }
}

abstract class ChildProcess(
    protected val binanceClient: BinanceClient,
    private val telegramClient: TelegramClient,
    propertiesHolder: PropertiesHolder
) : Process {
    val currency = propertiesHolder.currency
    val threshold = propertiesHolder.buyThreshold
    val tradeSize = propertiesHolder.tradeSize
    val state: ChildProcessState

    init {
        val price = binanceClient.getPrice(currency)
        state = ChildProcessState(
            currentPrice = price,
            stopPrice = calculateStopPrice(price),
            status = NEW
        )
    }

    override fun completed(): Boolean {
        return state.status == COMPLETED
    }

    override fun getPrice(): Double {
        return state.stopPrice
    }

    override fun alive() {
        notifyState("LIVENESS")
    }

    override fun start() {
        val order = createOrder(state.stopPrice)
        state.status = PROGRESS
        state.orderId = order.orderId
        notifyState("START")
    }

    override fun update() {
        when (state.status) {
            NEW, PROGRESS -> completeOrTryChangePrice()
            COMPLETED -> return
        }
    }

    private fun completeOrTryChangePrice() {
        if (checkCompleted()) {
            complete()
        } else {
            updatePrices()
        }
    }

    private fun checkCompleted(): Boolean = binanceClient.getOrder(currency, state.orderId).status == "FILLED"
    private fun complete() {
        state.status = COMPLETED
        notifyState("COMPLETED")
    }

    private fun updatePrices() {
        val currentPrice = binanceClient.getPrice(currency)
        state.currentPrice = currentPrice
        if (shouldRecreateOrder(currentPrice)) {
            recreateOrder(currentPrice)
            notifyState("ORDER RECREATED")
        }
    }

    private fun recreateOrder(currentPrice: Double) {
        val stopPrice = calculateStopPrice(currentPrice)
        val newOrder = createOrder(stopPrice)
        binanceClient.deleteOrder(currency, state.orderId)
        state.orderId = newOrder.orderId
        state.stopPrice = stopPrice
    }

    private fun notifyState(event: String) {
        telegramClient.sendNotification("[$event] ${type()} PROCESS STATE($state)")
    }

    abstract fun createOrder(stopPrice: Double): OrderResponse
    abstract fun shouldRecreateOrder(currentPrice: Double): Boolean
    abstract fun calculateStopPrice(price: Double): Double
    abstract fun type(): String
}

class BuyProcess(
    binanceClient: BinanceClient,
    telegramClient: TelegramClient,
    propertiesHolder: PropertiesHolder
) : ChildProcess(binanceClient, telegramClient, propertiesHolder) {

    override fun createOrder(stopPrice: Double): OrderResponse =
        binanceClient.createBuyOrder(currency, stopPrice, tradeSize)

    override fun shouldRecreateOrder(currentPrice: Double) =
        currentPrice < state.stopPrice - threshold

    override fun calculateStopPrice(price: Double) = price + threshold

    override fun type() = "BUY"

}

class SellProcess(
    binanceClient: BinanceClient,
    telegramClient: TelegramClient,
    propertiesHolder: PropertiesHolder
) : ChildProcess(binanceClient, telegramClient, propertiesHolder) {

    override fun createOrder(stopPrice: Double): OrderResponse =
        binanceClient.createSellOrder(currency, stopPrice, tradeSize)

    override fun shouldRecreateOrder(currentPrice: Double) =
        currentPrice > state.stopPrice + threshold

    override fun calculateStopPrice(price: Double) = price - threshold

    override fun type() = "SELL"
}
