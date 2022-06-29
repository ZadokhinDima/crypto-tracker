package com.zadokhin.bitcointracker

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class TelegramClient(val restTemplate: RestTemplate) {

    @Value("\${bot.token}")
    val botToken: String = ""
    @Value("\${bot.notification.chat}")
    val chatId: Long = 0

    fun sendNotification(text: String) {
        val telegramBotApiUrl = "https://api.telegram.org"
        val method = "sendMessage"
        val fullUrl = "$telegramBotApiUrl/bot$botToken/$method"
        restTemplate.postForEntity(fullUrl, SendMessageRequest(chatId = chatId, text = text), Any::class.java)
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SendMessageRequest(
    @JsonProperty("chat_id") val chatId: Long,
    val text: String,
    @JsonProperty("reply_markup") val replyMarkup: KeyboardMarkup? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class KeyboardMarkup(
    @JsonProperty("inline_keyboard")
    val keyboard: List<List<KeyboardButton>>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class KeyboardButton(
    val text: String,
    @JsonProperty("callback_data")
    val callback: String
)
