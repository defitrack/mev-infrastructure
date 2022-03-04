package io.defitrack.mev.liquidation

import io.defitrack.mev.chains.contract.EvmContract
import io.defitrack.mev.chains.contract.EvmContractAccessor
import io.defitrack.mev.chains.contract.EvmContractAccessor.Companion.toAddress
import io.defitrack.mev.chains.contract.EvmContractAccessor.Companion.toUint256
import org.web3j.abi.datatypes.Function
import java.math.BigInteger

class AaveLiquidatorContract(
    evmContractAccessor: EvmContractAccessor,
    abi: String,
    address: String
) : EvmContract(
    evmContractAccessor, abi, address
) {

    fun liquidateFunction(
        debt: String,
        collateral: String,
        user: String,
        debtToPay: BigInteger
    ): Function {
        return createFunction(
            "liquidate",
            listOf(
                debt.toAddress(),
                collateral.toAddress(),
                user.toAddress(),
                debtToPay.toUint256()
            ),
            outputs = emptyList()
        )
    }

}