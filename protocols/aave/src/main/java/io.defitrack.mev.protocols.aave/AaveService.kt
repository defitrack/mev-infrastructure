package io.defitrack.mev.protocols.aave

import io.defitrack.mev.chains.polygon.config.PolygonContractAccessor
import io.defitrack.mev.common.abi.ABIResource
import org.springframework.stereotype.Component

@Component
class AaveService(
    private val polygonContractAccessor: PolygonContractAccessor,
    private val abiResource: ABIResource
) {

    fun getLendingPoolAddressesProviderContract(): LendingPoolAddressProviderContract {
        return LendingPoolAddressProviderContract(
            polygonContractAccessor,
            abiResource.getABI("aave/LendingPoolAddressesProvider.json"),
                "0xd05e3E715d945B59290df0ae8eF85c1BdB684744"
        )
    }

    fun getLendingPoolDataProviderContract(): ProtocolDataProviderContract {
        return ProtocolDataProviderContract(
            polygonContractAccessor,
            abiResource.getABI("aave/ProtocolDataProvider.json"),
                "0x7551b5D2763519d4e37e8B81929D336De671d46d"
        )
    }

    val lendingPoolContract by lazy {
        LendingPoolContract(
            polygonContractAccessor, abiResource.getABI("aave/LendingPool.json"),
            getLendingPoolAddressesProviderContract().lendingPoolAddress()
        )
    }

    val oracleContract by lazy {
        OracleContract(
            polygonContractAccessor,
            abiResource.getABI("aave/PriceOracle.json"),
            getLendingPoolAddressesProviderContract().priceOracleAddress()
        )
    }
}