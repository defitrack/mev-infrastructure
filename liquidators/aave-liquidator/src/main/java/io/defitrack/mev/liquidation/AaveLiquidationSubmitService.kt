package io.defitrack.mev.liquidation

import io.defitrack.mev.AaveLiquidationCallService
import io.defitrack.mev.user.domain.AaveUser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class AaveLiquidationSubmitService(
    private val aaveLiquidationCallService: AaveLiquidationCallService
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
                log.debug("Submitted tx: {}", submitTransaction)
            }
        } catch (ex: Exception) {
            log.error(ex.message)
        }
    }

    private fun submitTransaction(signedMessageAsHex: String) {
        log.info("Submitting $signedMessageAsHex")
    }
}