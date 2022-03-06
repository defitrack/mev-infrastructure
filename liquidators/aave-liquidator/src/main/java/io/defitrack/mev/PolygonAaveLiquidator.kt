package io.defitrack.mev

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@SpringBootApplication
class PolygonAaveLiquidator {
}

fun main(args: Array<String>) {
    runApplication<PolygonAaveLiquidator>(*args)
}