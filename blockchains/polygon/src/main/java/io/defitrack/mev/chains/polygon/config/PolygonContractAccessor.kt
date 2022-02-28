package io.defitrack.mev.chains.polygon.config

import io.defitrack.mev.chains.evm.abi.AbiDecoder
import io.defitrack.mev.chains.contract.EvmContractAccessor
import io.defitrack.mev.chains.web3.EvmGateway
import io.defitrack.mev.common.network.Network
import org.springframework.stereotype.Component

@Component
class PolygonContractAccessor(abiDecoder: AbiDecoder, val polygonGateway: PolygonGateway) :
    EvmContractAccessor(abiDecoder) {

    override fun getMulticallContract(): String {
        return "0x11ce4B23bD875D7F5C6a31084f55fDe1e9A87507"
    }

    override fun getNetwork(): Network {
        return Network.POLYGON
    }

    override fun getGateway(): EvmGateway {
        return polygonGateway
    }
}