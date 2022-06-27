package com.zadokhin.bitcointracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class BitcoinTrackerApplication {

}

fun main(args: Array<String>) {
    runApplication<BitcoinTrackerApplication>(*args)
}
