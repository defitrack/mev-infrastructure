package io.defitrack.mev

import io.defitrack.mev.chains.polygon.config.PolygonContractAccessor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.future.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class EthereumCheapBlockProducer(private val polygonContractAccessor: PolygonContractAccessor) {

    suspend fun produceBlocks(action: (BigInteger) -> Unit) = coroutineScope {
        var latestBlock: BigInteger = BigInteger.ZERO
        while (true) {
            this.ensureActive()
            try {
                val thisBlock =
                    polygonContractAccessor.getGateway().web3j().ethBlockNumber().sendAsync().await().blockNumber
                if (latestBlock != thisBlock) {
                    latestBlock = thisBlock
                    action(thisBlock)
                }
            } catch (ex: Exception) {
                logger.error("Error trying to produce blocks", ex)
            }
            delay(500)
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}