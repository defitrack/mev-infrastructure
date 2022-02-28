package io.defitrack.mev.liquidation

import io.defitrack.mev.protocols.aave.ReserveToken
import java.math.BigInteger

class Debt(
    val asset: ReserveToken,
    val totalAmount: BigInteger,
    val liquidatableInEth: BigInteger,
    val liquidatableDebt: BigInteger,
    val assetPrice: BigInteger,
)