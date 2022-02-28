package io.defitrack.mev

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class PolygonAaveLiquidator

fun main(args: Array<String>) {
    runApplication<PolygonAaveLiquidator>(*args)
}