package io.defitrack.mev.chains.evm.abi.domain

import io.defitrack.mev.chains.evm.abi.domain.AbiContractElement

data class AbiContractFallback(val stateMutability: String) : AbiContractElement()
