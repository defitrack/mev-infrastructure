package io.defitrack.mev.protocols.aave

import io.defitrack.mev.chains.contract.EvmContract
import io.defitrack.mev.chains.contract.EvmContractAccessor
import io.defitrack.mev.chains.contract.EvmContractAccessor.Companion.toAddress
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.runBlocking
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigInteger
import kotlin.time.Duration.Companion.minutes

class OracleContract(evmContractAccessor: EvmContractAccessor, abi: String, address: String) :
    EvmContract(
        evmContractAccessor, abi, address
    ) {

    val priceCache = Cache.Builder().expireAfterWrite(1.minutes).build<String, BigInteger>()

    fun getPrice(
        asset: String
    ): BigInteger {
        return runBlocking {
            priceCache.get(asset) {
                read(
                    "getAssetPrice",
                    listOf(
                        asset.toAddress()
                    ),
                    listOf(TypeReference.create(Uint256::class.java))
                )[0].value as BigInteger
            }
        }
    }
}