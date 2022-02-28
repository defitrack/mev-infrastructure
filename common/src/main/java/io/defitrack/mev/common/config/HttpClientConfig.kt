package io.defitrack.mev.common.config

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HttpClientConfig {

    @Bean
    fun provideClient(): HttpClient {
        return HttpClient(Apache) {
            install(JsonFeature) {
                serializer = GsonSerializer()
                acceptContentTypes = acceptContentTypes + ContentType("application", "json+hal")
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60000
                connectTimeoutMillis = 60000
                socketTimeoutMillis = 60000
            }
        }
    }
}