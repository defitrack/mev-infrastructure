package io.defitrack.mev.liquidation

import io.defitrack.mev.common.FormatUtilsExtensions.asEth
import io.defitrack.mev.protocols.aave.AaveService
import io.defitrack.mev.protocols.aave.UserAccountData
import io.defitrack.mev.user.UserService
import io.defitrack.mev.user.domain.AaveUser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class UnhealthyUserLiquidator(
    private val userService: UserService,
    private val aaveService: AaveService,
    private val aaveLiquidationSubmitService: AaveLiquidationSubmitService,
    private val aaveLiquidationPrepareService: AaveLiquidationPrepareService,
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private fun unhealthyUsers() = userService.getRiskyUsers()

    @Scheduled(fixedDelay = 1000L)
    private fun handleRiskyUsers() {
        try {
            unhealthyUsers()
                .filter {
                    !it.ignored
                }
                .parallelStream()
                .forEach { user ->
                    handleUnhealthyUser(user)
                }
        } catch (ex: Exception) {
            log.error(ex.message)
        }
        log.info("done handling unhealthy users")
    }

    private fun handleUnhealthyUser(user: AaveUser) {
        try {
            val hf = getHealthFactor(user)
            if (hf < 1.0) {
                aaveLiquidationSubmitService.liquidate(user)
            } else {
                aaveLiquidationPrepareService.prepare(user);
            }
        } catch (ex: Exception) {
            log.error(ex.message)
        }
    }


    private fun getHealthFactor(user: AaveUser) = getUserAccountData(user.address)?.healthFactor?.asEth() ?: -1.0


    private fun getUserAccountData(
        user: String
    ): UserAccountData? {
        return try {
            aaveService.lendingPoolContract.getUserAccountData(user)
        } catch (exception: Exception) {
            exception.printStackTrace()
            null
        }
    }
}