package io.defitrack.polygon.config

import io.defitrack.evm.web3j.EvmGateway
import okhttp3.OkHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.websocket.WebSocketClient
import org.web3j.protocol.websocket.WebSocketService
import java.net.URI
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Component
class PolygonGateway(@Value("\${io.defitrack.polygon.endpoint.url}") private val endpoint: String) : EvmGateway {

    @Scheduled(fixedRate = 20000)
    fun scheduledTask() {
        try {
            assureConnection()
        } catch (ex: Exception) {
            logger.error("Unable to reconnect polygon websocket", ex)
        }
    }

    override fun web3j(): Web3j {
        assureConnection()
        return provideWeb3()
    }

    protected var webSocketClient: WebSocketClient? = null

    fun assureConnection() {
        try {
            webSocketClient?.let {
                if (!it.isOpen) {
                    it.reconnectBlocking()
                }
            }
        } catch (ex: Exception) {
            logger.error("Unable to reconnect to websocket")
            throw ex
        }
    }

    @Primary
    fun provideWeb3(): Web3j {
        return if (endpoint.startsWith("ws")) {
            this.webSocketClient = WebSocketClient(URI.create(endpoint))
            val webSocketService = WebSocketService(webSocketClient, false)
            webSocketService.connect({

            }, {
                logger.error("An error occurred in secondary websocket", it)
                assureConnection()
            }, {
                logger.info("Websocket connection closed")
            })
            Web3j.build(webSocketService)
        } else {
            val builder = OkHttpClient.Builder()
            builder.connectTimeout(20, TimeUnit.SECONDS)
            builder.writeTimeout(60, TimeUnit.SECONDS)
            builder.readTimeout(60, TimeUnit.SECONDS)
            builder.callTimeout(60, TimeUnit.SECONDS)
            val httpService = HttpService(endpoint, false)
            return Web3j.build(httpService, 5L, ScheduledThreadPoolExecutor(5))
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}