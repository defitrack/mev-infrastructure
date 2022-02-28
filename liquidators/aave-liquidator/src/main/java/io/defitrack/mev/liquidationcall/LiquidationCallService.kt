package io.defitrack.mev

import io.defitrack.mev.liquidationcall.AaveLiquidationCall
import io.defitrack.mev.user.domain.AaveUser
import io.defitrack.mev.liquidationcall.AaveLiquidationCallRepository
import io.defitrack.mev.user.AaveUserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AaveLiquidationCallService(
    private val aaveLiquidationCallRepository: AaveLiquidationCallRepository,
    private val aaveUserRepository: AaveUserRepository
) {

    @Transactional(readOnly = true)
    fun findUnsubmittedAndProfitableByUser(aaveUser: AaveUser): AaveLiquidationCall? =
        aaveLiquidationCallRepository.findUnusedByUser(aaveUser.address)?.takeIf {
            it.netProfit > 0
        }

    @Transactional
    fun saveAaveLiquidationCall(aaveLiquidationCall: AaveLiquidationCall) {
        val user = aaveUserRepository.findByIdOrNull(aaveLiquidationCall.aaveUser.address)!!
        aaveLiquidationCallRepository.findUnusedByUser(user.address)?.let(aaveLiquidationCallRepository::delete)
        aaveLiquidationCallRepository.save(
            aaveLiquidationCall.copy(
                aaveUser = user
            )
        )
    }

    @Transactional
    fun updateLiquidationCall(aaveLiquidationCall: AaveLiquidationCall) {
        aaveLiquidationCallRepository.save(
            aaveLiquidationCall
        )
    }
}