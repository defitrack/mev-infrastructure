package io.defitrack.mev.liquidation

import io.defitrack.mev.chains.contract.multicall.MultiCallElement
import io.defitrack.mev.chains.polygon.config.PolygonContractAccessor
import io.defitrack.mev.common.FormatUtilsExtensions.asEth
import io.defitrack.mev.protocols.aave.AaveService
import io.defitrack.mev.protocols.aave.UserAccountData
import io.defitrack.mev.user.UserService
import io.defitrack.mev.user.domain.AaveUser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.util.concurrent.Executors

@Component
class HealthFactorUpdater(
    private val userService: UserService,
    private val aaveService: AaveService,
    private val polygonContractAccessor: PolygonContractAccessor
) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(fixedDelay = 2000)
    fun updateUserHFs() {
        val chunked = userService.getAlllUsers().chunked(500)
        chunked.parallelStream().forEach {
            updateHfs(it)
        }
        log.info("done updating user hf")
    }

    private fun updateHfs(it: List<AaveUser>) {
        try {
            getHealthFactor(it).forEachIndexed { index, result ->
                updateHf(it[index], result.healthFactor?.asEth() ?: -1.0)
            }
        } catch (ex: Exception) {
            log.error("unable to update hf: {}", ex.message)
        }
    }

    private fun updateHf(
        user: AaveUser,
        hf: Double
    ) {
        userService.updateUserHF(user, hf)
    }

    private fun getHealthFactor(users: List<AaveUser>) = getUserAccountData(users.map {
        it.address
    })

    private fun getUserAccountData(
        users: List<String>
    ): List<UserAccountData> {
        return polygonContractAccessor.readMultiCall(
            users.map {
                MultiCallElement(
                    aaveService.lendingPoolContract.getUserAccountDataFunction(it),
                    aaveService.lendingPoolContract.address
                )
            }
        ).map {
            with(it) {
                UserAccountData(
                    this[0].value as BigInteger,
                    this[1].value as BigInteger,
                    this[2].value as BigInteger,
                    this[3].value as BigInteger,
                    this[4].value as BigInteger,
                    this[5].value as BigInteger
                )
            }
        }
    }
}