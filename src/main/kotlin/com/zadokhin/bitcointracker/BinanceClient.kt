package com.zadokhin.bitcointracker

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.Instant

@Service
class BinanceClient(private val restTemplate: RestTemplate, private val cryptoService: CryptoService) {

    @Value("\${binance.api.host}")
    private val binanceHost: String = ""
    @Value("\${binance.api.key}")
    private val binanceApiKey: String = ""

    fun getLastTrades(currency: String, limit: Int): Array<BinanceTrade> {
        val fullUrl = "$binanceHost/api/v3/trades?symbol=$currency&limit=$limit"
        try {
            val response = restTemplate.getForEntity(fullUrl, Array<BinanceTrade>::class.java)
            return if (response.body != null) response.body!! else arrayOf()
        } catch (exception: HttpClientErrorException) {
            println("ERROR: $currency")
            throw exception
        }
    }

    fun getAccountInfo(): AccountInfo {
        val url = "api/v3/account"
        val timestamp = Instant.now().toEpochMilli()
        val queryText = "timestamp=$timestamp"
        val requestEntity = createRequestEntityWithApiKeyAndSignature(url, queryText, HttpMethod.GET)
        val response = restTemplate.exchange(requestEntity, AccountInfo::class.java)
        return response.body!!
    }

    fun getPrice(currency: String): Double {
        val fullUrl = "$binanceHost/api/v3/ticker/price?symbol=$currency"
        try {
            val response = restTemplate.getForEntity(fullUrl, CurrentPriceResponse::class.java)
            return response.body!!.price
        } catch (exception: HttpClientErrorException) {
            println("ERROR: $currency")
            throw exception
        }
    }

    fun getLastBitcoinVolumes(currency: String, batchSize: Int): List<Double> {
        val fullUrl = "$binanceHost/api/v3/klines?symbol=$currency&limit=$batchSize&interval=5m"
        try {
            val response = restTemplate.getForEntity(fullUrl, Array<out Array<out Any>>::class.java)
            return if (response.body != null) {
                return response.body!!.map { it[5].toString().toDouble() }
            } else listOf()
        } catch (exception: HttpClientErrorException) {
            println("ERROR: $currency")
            throw exception
        }
    }

    fun createBuyOrderMarketPrice(currency: String, qty: Double): OrderResponse {
        val url = "api/v3/order"
        val timestamp = Instant.now().toEpochMilli()
        val queryText = "symbol=$currency" +
                "&side=BUY" +
                "&type=MARKET" +
                "&quantity=$qty" +
                "&newOrderRespType=RESULT" +
                "&timestamp=$timestamp"

        val requestEntity = createRequestEntityWithApiKeyAndSignature(url, queryText, HttpMethod.POST)

        val responseEntity = restTemplate.exchange(requestEntity, OrderResponse::class.java)
        return responseEntity.body !!
    }

    fun createSellOrderMarketPrice(currency: String, qty: Double): OrderResponse {
        val url = "api/v3/order"
        val timestamp = Instant.now().toEpochMilli()
        val queryText = "symbol=$currency" +
                "&side=SELL" +
                "&type=MARKET" +
                "&quantity=$qty" +
                "&newOrderRespType=RESULT" +
                "&timestamp=$timestamp"

        val requestEntity = createRequestEntityWithApiKeyAndSignature(url, queryText, HttpMethod.POST)

        val responseEntity = restTemplate.exchange(requestEntity, OrderResponse::class.java)
        return responseEntity.body !!
    }

    fun createBuyOrder(currency: String, price: Double): OrderResponse {
        val url = "api/v3/order"
        val timestamp = Instant.now().toEpochMilli()
        val queryText = "symbol=$currency" +
                "&side=BUY" +
                "&type=STOP_LOSS_LIMIT" +
                "&stopPrice=${price + 100}" +
                "&price=$price" +
                "&timeInForce=GTC" +
                "&quantity=0.001" +
                "&recvWindow=5000" +
                "&newOrderRespType=RESULT" +
                "&timestamp=$timestamp"

        val requestEntity = createRequestEntityWithApiKeyAndSignature(url, queryText, HttpMethod.POST)

        val responseEntity = restTemplate.exchange(requestEntity, OrderResponse::class.java)
        return responseEntity.body !!
    }

    fun deleteOrder(currency: String, orderId: Long): OrderResponse {
        val timestamp = Instant.now().toEpochMilli()
        val queryText = "symbol=$currency" +
                "&orderId=$orderId" +
                "&recvWindow=50000" +
                "&timestamp=$timestamp"
        val url = "api/v3/order"

        val requestEntity =
            createRequestEntityWithApiKeyAndSignature(url, queryText, HttpMethod.DELETE)

        val responseEntity = restTemplate.exchange(requestEntity, OrderResponse::class.java)
        return responseEntity.body !!
    }

    private fun getHeadersWithApiKey(): HttpHeaders {
        val httpHeaders = HttpHeaders()
        httpHeaders["X-MBX-APIKEY"] = binanceApiKey
        return httpHeaders
    }

    private fun createRequestEntityWithApiKeyAndSignature(url: String, queryText: String, method: HttpMethod): RequestEntity<String> {
        val signature = cryptoService.getHmacSignature(queryText)
        val fullUrl = "$binanceHost/$url?$queryText&signature=$signature"
        return RequestEntity<String>(getHeadersWithApiKey(), method, URI.create(fullUrl))
    }
}

data class CurrentPriceResponse(val symbol: String, val price: Double)

data class BinanceTrade(
    val id: Long,
    val price: Double,
    val qty: Double,
    val quoteQty: Double,
    val time: Long,
    val isBuyerMaker: Boolean,
    val isBestMatch: Boolean
)

data class OrderResponse(
    val symbol: String,
    val orderId: Long,
    val orderListId: Long,
    val clientOrderId: String,
    val transactTime: Long,
    val price: Double,
    val origQty: Double,
    val executedQty: Double,
    val cummulativeQuoteQty: Double,
    val status: String,
    val timeInForce: String,
    val type: String,
    val side: String
)

data class AccountInfo(
    val makerCommission: Double,
    val takerCommission: Double,
    val buyerCommission: Double,
    val sellerCommission: Double,
    val canTrade: Boolean,
    val canWithdraw: Boolean,
    val canDeposit: Boolean,
    val updateTime: Long,
    val balances: List<BalanceInfo>
)

data class BalanceInfo(
    val asset : String,
    val free: Double,
    val locked: Double
)