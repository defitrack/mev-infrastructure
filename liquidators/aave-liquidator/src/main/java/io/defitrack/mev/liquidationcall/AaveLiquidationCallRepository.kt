package io.defitrack.mev.liquidationcall

import okhttp3.internal.toImmutableList
import org.springframework.stereotype.Service
import java.util.*

@Service
class AaveLiquidationCallRepository {

    val liquidationCalls = mutableListOf<AaveLiquidationCall>()
    fun findUnusedByUser(address: String): AaveLiquidationCall? {
        val copy = liquidationCalls.toImmutableList()
        return copy.filter {
            it.aaveUser.address.lowercase() == address.lowercase()
        }.firstOrNull {
            !it.submitted
        }
    }

    fun save(liquidationCall: AaveLiquidationCall) {
        if (liquidationCall.id == null) {
            this.liquidationCalls.add(
                liquidationCall.copy(
                    id = UUID.randomUUID().toString()
                )
            )
        } else {
            val copy = liquidationCalls.toImmutableList()
            copy.filter {
                liquidationCall.id == it.id
            }.forEach {
                this.liquidationCalls.remove(it)
            }
        }
    }

    fun delete(liquidationCall: AaveLiquidationCall) {
        liquidationCalls.remove(liquidationCall)
    }
}