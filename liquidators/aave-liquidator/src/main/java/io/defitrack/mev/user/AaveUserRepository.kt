package io.defitrack.mev.user

import io.defitrack.mev.user.domain.AaveUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AaveUserRepository : JpaRepository<AaveUser, String> {

    @Query("select u from AaveUser u where latestHf <= :hf and ignored = false order by latestHf")
    fun findRiskyUsers(@Param("hf") hf: Double): List<AaveUser>
}