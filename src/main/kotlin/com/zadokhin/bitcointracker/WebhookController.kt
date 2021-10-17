package com.zadokhin.bitcointracker

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController("/")
class WebhookController {

    @PostMapping("/webhook")
    fun catchTrigger(@RequestBody webhook: WebhookInfo): WebhookInfo {
        println(webhook)
        return webhook
    }

}

data class WebhookInfo (val trigger: TriggerType)

enum class TriggerType {
    LOWER
}
