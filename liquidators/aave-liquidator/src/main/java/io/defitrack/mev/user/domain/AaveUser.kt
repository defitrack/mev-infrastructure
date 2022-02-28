package io.defitrack.mev.domain


import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table


@Table(name = "aave_user")
@Entity
data class AaveUser(
    @Id val address: String,
    @Column(name = "latest_hf")
    val latestHf: Double = 2.0,
    val ignored: Boolean = false
)