package io.defitrack.mev

import io.defitrack.mev.protocols.aave.ReserveToken
import java.math.BigInteger

class Collateral(
    val asset: ReserveToken,
    val totalAmount: BigInteger,
    val valueInEth: BigInteger,
    val assetPrice: BigInteger,
)