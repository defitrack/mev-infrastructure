package io.defitrack.mev.user

import io.defitrack.mev.protocols.aave.AaveService
import io.reactivex.disposables.Disposable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(value = ["flags.new-user-monitor=true"], havingValue = "true", matchIfMissing = true)
class AaveUserMonitor(
    private val aaveUserService: UserService,
    private val aaveService: AaveService
) {

    var depositSub: Disposable? = null
    var borrowSub: Disposable? = null
    var withdrawSub: Disposable? = null

    @Scheduled(fixedRate = 10000)
    private fun liveEvents() {
        if (borrowSub == null || borrowSub!!.isDisposed) {
            log.info("restarting borrow sub")
            borrowSub = listenToEventWithName("Borrow")
        }
        if (depositSub == null || depositSub!!.isDisposed) {
            log.info("restarting deposit sub")
            depositSub = listenToEventWithName("Deposit")
        }
        if (withdrawSub == null || withdrawSub!!.isDisposed) {
            log.info("restarting withdraw sub")
            withdrawSub = listenToEventWithName("Withdraw")
        }
//        listenToEventWithName("LiquidationCall") {
//            println("LIQUIDATIONCALL!!!!!!")
//            println(it["liquidator"])
//        }
    }

    private fun listenToEventWithName(eventName: String): Disposable {
        val listenToEvents = aaveService.lendingPoolContract.listenToEvents(
            eventName
        )

        return listenToEvents.subscribe({ data ->
            saveUser(data)
        }) { error ->
            log.error("Unable to listen to $eventName")
            println(error.message)
        }
    }

    private fun saveUser(eventData: Map<String, Any>) {
        val user = eventData["user"] as String
        log.trace("Saving $user")
        aaveUserService.saveUser(user)
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}