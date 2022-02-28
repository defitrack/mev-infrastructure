package io.defitrack.mev.protocols.aave

import io.defitrack.mev.chains.contract.EvmContract
import io.defitrack.mev.chains.contract.EvmContractAccessor
import io.defitrack.mev.chains.contract.EvmContractAccessor.Companion.toAddress
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigInteger

class LendingPoolContract(evmContractAccessor: EvmContractAccessor, abi: String, address: String) :
    EvmContract(
        evmContractAccessor, abi, address
    ) {


    fun getUserAccountData(user: String): UserAccountData {
        val retVal = read(
            method = "getUserAccountData",
            inputs = listOf(
                user.toAddress()
            ),
            outputs = listOf(
                org.web3j.abi.TypeReference.create(Uint256::class.java),
                org.web3j.abi.TypeReference.create(Uint256::class.java),
                org.web3j.abi.TypeReference.create(Uint256::class.java),
                org.web3j.abi.TypeReference.create(Uint256::class.java),
                org.web3j.abi.TypeReference.create(Uint256::class.java),
                org.web3j.abi.TypeReference.create(Uint256::class.java),
            )
        )
        return with(retVal) {
            UserAccountData(
                this[0].value as BigInteger,
                this[1].value as BigInteger,
                this[2].value as BigInteger,
                this[3].value as BigInteger,
                this[4].value as BigInteger,
                this[5].value as BigInteger
            )
        }
    }
}