package io.defitrack.mev.chains.web3

import org.web3j.protocol.Web3j

interface EvmGateway {
    fun web3j(): Web3j
}