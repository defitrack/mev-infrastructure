package io.defitrack.mev.liquidator.aave

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PolygonAaveLiquidator

fun main(args: Array<String>) {
    runApplication<PolygonAaveLiquidator>(*args)
}