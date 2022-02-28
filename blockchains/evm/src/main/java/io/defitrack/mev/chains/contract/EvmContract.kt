package io.defitrack.mev.chains.contract

import io.defitrack.mev.chains.evm.abi.domain.AbiContractEvent
import io.reactivex.Flowable
import org.web3j.abi.EventEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Event
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.DefaultBlockParameterNumber
import org.web3j.protocol.core.methods.request.EthFilter
import java.math.BigInteger

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

    fun listenToEvents(event: String): Flowable<Map<String, Any>> {
        evmContractAccessor.getEvent(abi, event)?.let { contractEvent ->
            val theEvent = Event(
                contractEvent.name,
                contractEvent.inputs
                    .map { EvmContractAccessor.fromDataTypes(it.type, it.indexed) }
                    .toList()
            )

            val ethFilter = EthFilter(
                DefaultBlockParameterName.LATEST,
                DefaultBlockParameterName.LATEST, address
            )
            ethFilter.addOptionalTopics(EventEncoder.encode(theEvent))
            return createEventListener(ethFilter, theEvent, contractEvent)
        } ?: return Flowable.empty()
    }


    private fun createEventListener(
        ethFilter: EthFilter,
        theEvent: Event,
        contractEvent: AbiContractEvent
    ): Flowable<Map<String, Any>> {
        return evmContractAccessor.getGateway().web3j().ethLogFlowable(ethFilter)
            .map { logs ->
                val indexedValues = ArrayList<Type<*>>()
                val nonIndexedValues = FunctionReturnDecoder.decode(
                    logs.data, theEvent.nonIndexedParameters
                )

                val indexedParameters = theEvent.indexedParameters
                for (i in indexedParameters.indices) {
                    val value = FunctionReturnDecoder.decodeIndexedValue(
                        logs.topics[i + 1], indexedParameters[i]
                    )
                    indexedValues.add(value)
                }

                val collect = indexedValues + nonIndexedValues

                contractEvent.inputs.associate { input -> input.name to collect[contractEvent.inputs.indexOf(input)].value }
            }
    }

}
