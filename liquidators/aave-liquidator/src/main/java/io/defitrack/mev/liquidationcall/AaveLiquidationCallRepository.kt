package io.defitrack.mev.liquidationcall

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AaveLiquidationCallRepository : JpaRepository<AaveLiquidationCall, Long> {

    @Query("select alq from AaveLiquidationCall alq where submitted = false and aaveUser.address = :address")
    fun findUnusedByUser(@Param("address") address: String): AaveLiquidationCall?
}