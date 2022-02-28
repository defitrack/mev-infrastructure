package io.defitrack.mev.chains.evm.abi.domain

import com.fasterxml.jackson.annotation.JsonProperty

data class AbiContractReceive(val stateMutability: String,
                              @JsonProperty("payable")
                               val isPayable: Boolean = false) : AbiContractElement()
