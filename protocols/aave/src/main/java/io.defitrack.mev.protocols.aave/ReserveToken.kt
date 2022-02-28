package io.defitrack.mev.protocols.aave

import java.math.BigInteger

class ReserveToken(
    val name: String,
    val address: String,
    val decimals: Int,
    val liquidationBonus: BigInteger
)
