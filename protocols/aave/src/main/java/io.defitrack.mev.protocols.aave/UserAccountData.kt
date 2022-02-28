package io.defitrack.mev.protocols.aave

import java.math.BigInteger

class UserAccountData(
    val totalCollateralETH: BigInteger,
    val totalDebtETH: BigInteger,
    val availableBorrowsETH: BigInteger,
    val currentLiquidationThreshold: BigInteger,
    val ltv: BigInteger,
    val healthFactor: BigInteger
)
