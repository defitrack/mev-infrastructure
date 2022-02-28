package io.defitrack.mev.protocols.aave

import io.defitrack.mev.chains.contract.EvmContract
import io.defitrack.mev.chains.contract.EvmContractAccessor
import io.defitrack.mev.chains.contract.EvmContractAccessor.Companion.toAddress
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigInteger

class OracleContract(evmContractAccessor: EvmContractAccessor, abi: String, address: String) :
    EvmContract(
        evmContractAccessor, abi, address
    ) {
    fun getPrice(
        asset: String
    ): BigInteger {
        return read(
            "getAssetPrice",
            listOf(
                asset.toAddress()
            ),
            listOf(TypeReference.create(Uint256::class.java))
        )[0].value as BigInteger
    }
}