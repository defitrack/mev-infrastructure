package io.defitrack.evm.contract

import io.defitrack.evm.contract.multicall.MultiCallElement
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type

abstract class EvmContract(
    val evmContractAccessor: EvmContractAccessor,
    val abi: String,
    val address: String
) {

    fun call(function: Function): List<Type<*>> {
        return evmContractAccessor.executeCall(address, function)
    }

    fun createFunction(
        method: String,
        inputs: List<Type<*>> = emptyList(),
        outputs: List<TypeReference<out Type<*>>?>? = null
    ): Function {
        return EvmContractAccessor.createFunction(
            evmContractAccessor.getFunction(abi, method),
            inputs,
            outputs
        )
    }

    fun read(
        method: String,
        inputs: List<Type<*>> = emptyList(),
        outputs: List<TypeReference<out Type<*>>?>? = null
    ): List<Type<*>> {
        return evmContractAccessor.readFunction(
            address = address,
            inputs = inputs,
            outputs = outputs,
            function = evmContractAccessor.getFunction(
                abi,
                method
            )
        )
    }
}
