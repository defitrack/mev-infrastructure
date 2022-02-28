package io.defitrack.mev.chains.evm.abi.domain

import com.fasterxml.jackson.annotation.JsonProperty
import io.defitrack.mev.chains.evm.abi.domain.AbiContractElement

data class AbiContractReceive(val stateMutability: String,
                              @JsonProperty("payable")
                               val isPayable: Boolean = false) : AbiContractElement()
