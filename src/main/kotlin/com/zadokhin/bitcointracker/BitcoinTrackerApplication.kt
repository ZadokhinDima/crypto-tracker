package com.zadokhin.bitcointracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate

@SpringBootApplication
@EnableScheduling
class BitcoinTrackerApplication {

    @Bean
    fun restTemplate(): RestTemplate = RestTemplateBuilder().build()
}

fun main(args: Array<String>) {
    runApplication<BitcoinTrackerApplication>(*args)
}

