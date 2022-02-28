package io.defitrack.mev.chains.evm.abi

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackageClasses = [DefiTrackAbi::class])
class DefiTrackAbi