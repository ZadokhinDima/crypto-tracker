package com.zadokhin.bitcointracker.process


data class MainProcessState(
    var lastPrice: Double,
    var currentPrice: Double,
    var status: MainProcessStatus
)

enum class MainProcessStatus {
    CREATED, AFTER_BUY, AFTER_SELL, WAITING_BUY, WAITING_SELL
}

data class ChildProcessState(
    var currentPrice: Double,
    var stopPrice: Double,
    var orderId: Long = 0,
    var status: ChildProcessStatus
)

enum class ChildProcessStatus {
    NEW, PROGRESS, COMPLETED
}
