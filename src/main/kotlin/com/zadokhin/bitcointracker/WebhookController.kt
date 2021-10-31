package com.zadokhin.bitcointracker

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController("/")
class WebhookController(val strategiesService: StrategiesService) {

    @PostMapping("/webhook")
    fun catchTrigger(@RequestBody webhook: WebhookInfo): ResponseEntity<String> {
        println(webhook)
        if (webhook.trigger == TriggerType.LOWER) {
            strategiesService.startBB("BTCBUSD")
        }
        if (webhook.trigger == TriggerType.UPPER) {
            strategiesService.stopBB()
        }
        return ResponseEntity.ok("ok")
    }

}

data class WebhookInfo (val trigger: TriggerType)

enum class TriggerType {
    LOWER, UPPER
}
