package io.defitrack.evm.contract.multicall

import org.web3j.abi.datatypes.Function

data class MultiCallElement(
    val function: Function,
    val address: String
)