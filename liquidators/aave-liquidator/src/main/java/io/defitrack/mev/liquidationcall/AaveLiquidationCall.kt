package io.defitrack.mev.liquidationcall

import io.defitrack.mev.user.domain.AaveUser
import java.util.*

data class AaveLiquidationCall(
    val id: String? = null,
    val submittedAt: Date? = null,
    val submitted: Boolean = false,
    val unsignedData: String,
    val signedData: String,
    val aaveUser: AaveUser,
    val netProfit: Double
)