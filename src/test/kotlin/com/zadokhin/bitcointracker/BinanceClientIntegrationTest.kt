package com.zadokhin.bitcointracker

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles


class BinanceClientIntegrationTest(@Autowired val binanceClient: BinanceClient) {

    val currency = "BTCBUSD"

    @Test
    fun createOrderTest() {
        val orderResponse = binanceClient.createBuyOrder(currency, 10000.0, 0.1)
        val orderId = orderResponse.orderId
        assertThat(orderId).isNotNull
        val deleteOrderResponse = binanceClient.deleteOrder(currency, orderId)
        assertThat(deleteOrderResponse.orderId).isEqualTo(orderId)
    }

    @Test
    fun getWalletInfo() {
        val accountInfo = binanceClient.getAccountInfo()
        assertThat(accountInfo).isNotNull
    }

    @Test
    fun createBuyOrderMarketPrice() {
        val orderResponse = binanceClient.createBuyOrderMarketPrice(currency, qty = 0.01)
        assertThat(orderResponse.orderId).isNotNull
    }

}
