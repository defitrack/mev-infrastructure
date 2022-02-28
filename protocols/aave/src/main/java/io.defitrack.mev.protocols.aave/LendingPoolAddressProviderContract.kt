package io.defitrack.mev.protocols.aave

import io.defitrack.mev.chains.contract.EvmContract
import io.defitrack.mev.chains.contract.EvmContractAccessor
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address

class LendingPoolAddressProviderContract(evmContractAccessor: EvmContractAccessor, abi: String, address: String) :
    EvmContract(
        evmContractAccessor, abi, address
    ) {

    fun lendingPoolAddress(): String {
        return read(
            "getLendingPool",
            emptyList(),
            listOf(TypeReference.create(Address::class.java))
        )[0].value as String
    }

    fun priceOracleAddress(): String {
        return read(
            "getPriceOracle",
            emptyList(),
            listOf(TypeReference.create(Address::class.java))
        )[0].value as String
    }
}