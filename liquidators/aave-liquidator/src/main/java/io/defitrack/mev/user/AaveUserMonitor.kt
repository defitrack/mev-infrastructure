package io.defitrack.mev

import io.defitrack.mev.chains.polygon.config.PolygonContractAccessor
import io.defitrack.mev.protocols.aave.AaveService
import kotlinx.coroutines.coroutineScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.stream.Stream

@Component
class AaveUserMonitor(
    private val polygonContractAccessor: PolygonContractAccessor,
    private val aaveUserService: UserService,
    private val aaveService: AaveService
) {

    var userReserveMap = mutableMapOf<String, Set<String>>()

    suspend fun listen() = coroutineScope {
        liveEvents()
    }

    private fun liveEvents() {
        log.debug("listening to borrow, deposit, withdraw and liquidationcalls so we can save more users")
        listenToEventWithName("Borrow")
        listenToEventWithName("Deposit")
        listenToEventWithName("Withdraw")
        listenToEventWithName("LiquidationCall") {
            println("LIQUIDATIONCALL!!!!!!")
            println(it["liquidator"])
        }
    }

    private fun listenToEventWithName(eventName: String, doThings: (Map<String, Any>) -> Unit = saveUser()) {


        aaveService.getLendingpoolContract().listenToEvents(
            eventName
        ).subscribe(doThings) { error ->
            println(error.message)
        }
    }

    private fun saveUser(): (Map<String, Any>) -> Unit = {
        val currentSet = userReserveMap.getOrDefault(it["user"] as String, mutableSetOf())
        userReserveMap[it["user"] as String] =
            Stream.concat(currentSet.stream(), setOf(it["reserve"] as String).stream()).toList().toSet()
        aaveUserService.saveUser(it["user"] as String)
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}