package io.defitrack.mev.domain

import java.util.*
import javax.persistence.*

@Entity
@Table(name = "aave_liquidation_call")
data class AaveLiquidationCall(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Temporal(TemporalType.TIMESTAMP)
    val submittedAt: Date? = null,
    val submitted: Boolean = false,
    val unsignedData: String,
    val signedData: String,
    @OneToOne
    @JoinColumn(name = "user_id")
    val aaveUser: AaveUser,
    @Column(name = "netprofit")
    val netProfit: Double
)