package io.defitrack.evm.contract

import io.defitrack.evm.contract.EvmContractAccessor.Companion.toAddress
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigInteger

open class ERC20Contract(
    ethereumContractAccessor: EvmContractAccessor,
    abi: String,
    address: String
) :
    EvmContract(ethereumContractAccessor, abi, address) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun balanceOfFunction(address: String): Function {
        return createFunction(
            "balanceOf",
            inputs = listOf(address.toAddress()),
            outputs = listOf(
                TypeReference.create(Uint256::class.java)
            )
        )
    }

    fun balanceOf(address: String): BigInteger {
        return call(
            balanceOfFunction(address)
        )[0].value as BigInteger
    }

    val name by lazy {
        read("name")[0].value as String
    }

    val symbol by lazy {
        read("symbol")[0].value as String
    }

    val decimals by lazy {
        runBlocking {
            (read("decimals")[0].value as BigInteger).toInt()
        }
    }
}