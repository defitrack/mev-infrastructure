package io.defitrack.mev.chains.polygon.config

import io.defitrack.mev.chains.web3.EvmGateway
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

    protected var webSocketClient: WebSocketClient? = null
    protected val web3j: Web3j
    init {
        if (endpoint.startsWith("ws")) {
            this.webSocketClient = WebSocketClient(URI.create(endpoint))
            val webSocketService = WebSocketService(webSocketClient, false)
            webSocketService.connect({

            }, {
                logger.error("An error occurred in secondary websocket", it)
            }, {
                logger.info("Websocket connection closed")
            })
            web3j = Web3j.build(webSocketService)
        } else {
            val builder = OkHttpClient.Builder()
            builder.connectTimeout(20, TimeUnit.SECONDS)
            builder.writeTimeout(60, TimeUnit.SECONDS)
            builder.readTimeout(60, TimeUnit.SECONDS)
            builder.callTimeout(60, TimeUnit.SECONDS)
            val httpService = HttpService(endpoint, false)
            web3j = Web3j.build(httpService, 5L, ScheduledThreadPoolExecutor(5))
        }
    }

    @Scheduled(fixedRate = 5000)
    fun scheduledTask() {
        try {
            assureConnection()
        } catch (ex: Exception) {
            logger.error("Unable to reconnect polygon websocket", ex)
        }
    }

    override fun web3j(): Web3j {
        assureConnection()
        return web3j
    }

    fun assureConnection() {
        try {
            webSocketClient?.let {
                if (!it.isOpen || it.isClosed) {
                    logger.info("ws was closed, reopening")
                    it.reconnectBlocking()
                }
            }
        } catch (ex: Exception) {
            logger.error("Unable to reconnect to websocket")
            throw ex
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}