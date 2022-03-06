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
class HealthFactorUpdater(
    private val userService: UserService,
    private val aaveService: AaveService
) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(fixedDelay = 2000)
    fun updateUserHFs() {
        userService.getAlllUsers().forEach {
            val hf = getHealthFactor(it)
            userService.updateUserHF(it, hf)
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