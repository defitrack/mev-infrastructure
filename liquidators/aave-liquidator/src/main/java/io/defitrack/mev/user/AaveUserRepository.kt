package io.defitrack.mev

import io.defitrack.mev.domain.AaveUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AaveUserRepository : JpaRepository<AaveUser, String> {

    @Query("select u from AaveUser u where latestHf <= :hf and ignored = false order by latestHf")
    fun findRiskyUsers(@Param("hf") hf: Double): List<AaveUser>
}