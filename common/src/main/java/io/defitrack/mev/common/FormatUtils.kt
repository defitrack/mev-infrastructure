package io.defitrack.mev.common

import io.defitrack.common.utils.BigDecimalExtensions.dividePrecisely
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

object FormatUtils {
    fun asEth(weiBalance: BigInteger?): Double {
        return if (weiBalance != null) {
            BigDecimal(weiBalance).divide(
                BigDecimal.valueOf(Math.pow(10.0, 18.0)),
                18,
                RoundingMode.HALF_DOWN
            ).toDouble()
        } else {
            (-1).toDouble()
        }
    }

    fun asEth(weiBalance: BigDecimal?): Double {
        return weiBalance?.divide(
            BigDecimal.valueOf(Math.pow(10.0, 18.0)),
            18,
            RoundingMode.HALF_DOWN
        )?.toDouble()
            ?: (-1).toDouble()
    }


    fun asEth(weiBalance: BigInteger?, decimals: Int): BigDecimal {
        return BigDecimal(weiBalance).dividePrecisely(BigDecimal.TEN.pow(decimals))
    }

    fun asWei(ethBalance: Double): BigInteger {
        return BigInteger.valueOf((ethBalance * Math.pow(10.0, 18.0)).toLong())
    }

    fun asWei(ethBalance: Double, decimals: Int): BigInteger {
        return BigInteger.valueOf((ethBalance * Math.pow(10.0, decimals.toDouble())).toLong())
    }

}