package com.zadokhin.bitcointracker.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors


@Configuration
class RestTemplateConfig {

    @Bean
    fun restTemplate(): RestTemplate {
        val factory: ClientHttpRequestFactory = BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
        val restTemplate = RestTemplate(factory)
        restTemplate.interceptors.add(LoggingInterceptor())
        return restTemplate
    }
}

class LoggingInterceptor() : ClientHttpRequestInterceptor {

    val LOGGER: Logger = LoggerFactory.getLogger(LoggingInterceptor::class.java)

    override fun intercept(request: HttpRequest, reqBody: ByteArray, ex: ClientHttpRequestExecution): ClientHttpResponse {
        LOGGER.debug("Request body: {}", String(reqBody, StandardCharsets.UTF_8))
        val response: ClientHttpResponse = ex.execute(request, reqBody)
        val isr = InputStreamReader(
            response.body, StandardCharsets.UTF_8
        )
        val body = BufferedReader(isr).lines()
            .collect(Collectors.joining("\n"))
        LOGGER.debug("Response body: {}", body)
        return response
    }

}
