package io.defitrack.mev.liquidation

import io.defitrack.mev.AaveLiquidationCallService
import io.defitrack.mev.chains.polygon.config.PolygonContractAccessor
import io.defitrack.mev.user.domain.AaveUser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.DefaultBlockParameterNumber
import java.util.*

@Component
class AaveLiquidationSubmitService(
    private val aaveLiquidationCallService: AaveLiquidationCallService,
    private val polygonContractAccessor: PolygonContractAccessor,
    private val aaveLiquidationPrepareService: AaveLiquidationPrepareService
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    fun liquidate(user: AaveUser) {
        try {
            val unsubmitted = aaveLiquidationCallService.findUnsubmittedAndProfitableByUser(user)
            if (unsubmitted != null) {
                val submitTransaction = submitTransaction(unsubmitted.signedData)
                aaveLiquidationCallService.updateLiquidationCall(
                    unsubmitted.copy(
                        submitted = true,
                        submittedAt = Date()
                    )
                )
                val blockNumber = polygonContractAccessor.polygonGateway.web3j().ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().block.number
                log.info("Submitted tx: {} at {}", submitTransaction, blockNumber)
            } else {
                aaveLiquidationPrepareService.prepare(user)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            log.error("error trying to effectively liquidate user: {}", ex.message)
        }
    }

    private fun submitTransaction(signedMessageAsHex: String): String? {
        log.debug("submitting")
        val result =
            polygonContractAccessor.polygonGateway.web3j().ethSendRawTransaction(signedMessageAsHex).sendAsync().get()
        if (result.error != null) {
            log.error("polygon gateway issue: {}", result.error.message)
        }
        return result.transactionHash
    }
}