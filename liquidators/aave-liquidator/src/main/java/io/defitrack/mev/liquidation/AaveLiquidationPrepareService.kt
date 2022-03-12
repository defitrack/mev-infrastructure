package io.defitrack.mev.liquidation

import io.defitrack.mev.AaveLiquidationCallService
import io.defitrack.mev.chains.polygon.config.PolygonContractAccessor
import io.defitrack.mev.common.FormatUtils
import io.defitrack.mev.common.FormatUtilsExtensions.asEth
import io.defitrack.mev.common.abi.ABIResource
import io.defitrack.mev.liquidationcall.AaveLiquidationCall
import io.defitrack.mev.protocols.aave.AaveService
import io.defitrack.mev.protocols.aave.UserAccountData
import io.defitrack.mev.protocols.aave.UserReserveData
import io.defitrack.mev.user.UserService
import io.defitrack.mev.user.domain.AaveUser
import org.bouncycastle.util.encoders.Hex
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.web3j.abi.FunctionEncoder
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.core.DefaultBlockParameterName
import java.math.BigDecimal
import java.math.BigInteger
import javax.annotation.PostConstruct

@Component
class AaveLiquidationPrepareService(
    private val aaveLiquidationCallService: AaveLiquidationCallService,
    private val polygonContractAccessor: PolygonContractAccessor,
    private val userService: UserService,
    private val aaveService: AaveService,
    private val abiResource: ABIResource,
    @Value("\${io.defitrack.liquidator.private-key}") private val whitehatPriveKey: String,
    @Value("\${io.defitrack.liquidator.address}") private val whitehatAddress: String,
    @Value("\${io.defitrack.liquidator.contract-address}") private val liquidatorContractAddress: String
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val aaveLiquidatorContract = AaveLiquidatorContract(
        polygonContractAccessor,
        abiResource.getABI("defitrack/mev/liquidators/aave.json"),
        "0x41d1faf364d5ce754a6af1e574f746c3b9b4820b"
    );

    private val liquidationGasLimit = BigInteger.valueOf(2500000)

    private val protocolDataProviderContract = aaveService.getLendingPoolDataProviderContract()

    fun getUserReserveData(user: String): List<UserReserveData> {
        val tokens = protocolDataProviderContract.allReservesTokens
        return polygonContractAccessor.readMultiCall(
            tokens.map { token ->
                protocolDataProviderContract.getReserveDataFunction(user, token.address)
            }).asSequence()
            .mapIndexed { index, result ->
                with(result) {
                    UserReserveData(
                        this[0].value as BigInteger,
                        this[1].value as BigInteger,
                        this[2].value as BigInteger,
                        this[3].value as BigInteger,
                        this[4].value as BigInteger,
                        this[5].value as BigInteger,
                        this[6].value as BigInteger,
                        this[7].value as BigInteger,
                        this[8].value as Boolean,
                        tokens[index]
                    )
                }
            }.toList()
    }

    fun prepare(user: AaveUser) {
        val userReserveData = getUserReserveData(user.address)
        val allUserReserves = userReservesSortedByValue(userReserveData)
        val allUserDebts = getDebts(userReserveData)

        if (allUserReserves.isEmpty()) {
            return
        }

        val result = getBestDebtAndCollateral(allUserDebts, allUserReserves)

        val liquidationBonusInEth = getLiquidationBonusInEth(result)
        val actualProfit = liquidationBonusInEth.asEth() - transactionCostInEth()
        val healthFactor = getHealthFactor(user)
        if (actualProfit > 0 && liquidatable(healthFactor)) {
            logLiquidationPossibility(result, liquidationBonusInEth, user)
            val liquidate = liquidate(
                result.second.asset.address,
                result.first.asset.address,
                user.address,
                result.first.liquidatableDebt,
                result.first
            )


            val unsignedTransaction = constructTransaction(liquidate)
            val signedMessage = signTransaction(unsignedTransaction)
            val signedTransaction = prettify(Hex.toHexString(signedMessage))

            val submitted = submitTransaction(signedTransaction)
            log.info("Submitted tx: {}", submitted)
        } else {

            if (actualProfit < -0.3) {
                userService.setIgnored(user, true)
            } else {
                userService.setIgnored(user, false)
            }

            val liquidate = liquidate(
                result.second.asset.address,
                result.first.asset.address,
                user.address,
                result.first.liquidatableDebt,
                result.first
            )

            val unsignedTransaction = constructTransaction(liquidate)
            val signedMessage = signTransaction(unsignedTransaction)
            val signedTransaction = prettify(Hex.toHexString(signedMessage))
            aaveLiquidationCallService.saveAaveLiquidationCall(
                AaveLiquidationCall(
                    submitted = false,
                    unsignedData = unsignedTransaction.data,
                    signedData = signedTransaction,
                    aaveUser = user,
                    netProfit = actualProfit
                )
            )
        }
    }

    fun transactionCostInEth(): Double {
        val wmaticPerEth = aaveService.oracleContract.getPrice("0x0d500b1d8e8ef31e21c99d1db9a6444d3adf1270")
        val costInWmatic = (BigInteger.valueOf(50).times(BigInteger.TEN.pow(9)) * liquidationGasLimit).asEth()
        return wmaticPerEth.toBigDecimal().times(BigDecimal.valueOf(costInWmatic)).asEth()
    }

    private fun liquidatable(healthFactor: Double) = healthFactor < 1.0 && healthFactor > 0

    private fun submitTransaction(signedMessageAsHex: String): String? {
        log.debug("submitting")
        val result =
            polygonContractAccessor.polygonGateway.web3j().ethSendRawTransaction(signedMessageAsHex).sendAsync().get()
        if (result.error != null) {
            log.error(result.error.message)
        }
        return result.transactionHash
    }

    private fun liquidate(
        collateralAddress: String,
        assetToRepay: String,
        user: String,
        debtToCover: BigInteger,
        debt: Debt
    ): String {
        log.trace(
            """
            We can liquidate: 
            collateral: $collateralAddress
            asset to repay: $assetToRepay
            user: $user,
            debt to cover: $debtToCover
            debt: ${debt.asset.address}
        """.trimIndent()
        )

        val liquidationFunction = aaveLiquidatorContract.liquidateFunction(
            debt.asset.address,
            collateralAddress,
            user,
            debtToCover
        )
        return FunctionEncoder.encode(liquidationFunction)
    }

    private fun getHealthFactor(user: AaveUser) = getUserAccountData(user.address)?.healthFactor?.asEth() ?: -1.0

    private fun prettify(address: String): String {
        return if (!address.startsWith("0x")) {
            String.format("0x%s", address)
        } else {

            address
        }
    }


    private fun getUserAccountData(
        user: String
    ): UserAccountData? {
        return try {
            aaveService.lendingPoolContract.getUserAccountData(user)
        } catch (exception: Exception) {
            exception.printStackTrace()
            null
        }
    }

    private fun sign(keyPair: ECKeyPair, etherTransaction: RawTransaction): ByteArray {
        return TransactionEncoder.signMessage(etherTransaction, Credentials.create(keyPair))
    }

    private fun signTransaction(unsignedTransaction: RawTransaction) =
        sign(ECKeyPair.create(Hex.decode(whitehatPriveKey)), unsignedTransaction)

    private fun getLiquidationBonusInEth(result: Pair<Debt?, Collateral?>): BigInteger {
        return if (result.first == null || result.second == null) {
            BigInteger.ZERO
        } else {
            val liquidatable =
                calculateTotalPossibleLiquidatableInEth(result.first!!, result.second!!)
            val collateralToReceive = liquidatable
                .multiply(BigInteger.valueOf(result.second!!.asset.liquidationBonus.toLong()))
                .divide(
                    BigInteger.valueOf(10000)
                )
            collateralToReceive - liquidatable
        }
    }

    private fun getBestDebtAndCollateral(
        allUserDebts: List<Debt>,
        allUserReserves: List<Collateral>
    ): Pair<Debt, Collateral> = allUserDebts
        .map { debt ->
            debt to allUserReserves
                .reduce { acc, reserve ->
                    val l1 = calculateTotalPossibleLiquidatableInEth(debt, acc)
                    val l2 = calculateTotalPossibleLiquidatableInEth(debt, reserve)
                    if (l1 > l2) acc else reserve
                }
        }.reduce { acc, pair ->
            val l1 = calculateTotalPossibleLiquidatableInEth(acc.first, acc.second)
            val l2 = calculateTotalPossibleLiquidatableInEth(pair.first, pair.second)
            if (l1 > l2) acc else pair
        }

    private fun constructTransaction(
        liquidate: String
    ) = RawTransaction.createTransaction(
        137L,
        polygonContractAccessor.polygonGateway.web3j()
            .ethGetTransactionCount(whitehatAddress, DefaultBlockParameterName.LATEST)
            .send().transactionCount,
        liquidationGasLimit,
        liquidatorContractAddress,
        BigInteger.ZERO,
        liquidate,
        BigInteger.valueOf(50).times(BigInteger.TEN.pow(9)),
        BigInteger.valueOf(50).times(BigInteger.TEN.pow(9))
    )


    private fun calculateTotalPossibleLiquidatableInEth(debt: Debt, collateral: Collateral): BigInteger {
        val collateralValue = collateral.valueInEth
        val debtValue = debt.liquidatableInEth
        return if (collateralValue > debtValue) {
            debt.liquidatableInEth
        } else {
            collateralValue
        }
    }

    private fun logLiquidationPossibility(
        result: Pair<Debt, Collateral>,
        possibleEarning: BigInteger,
        user: AaveUser
    ) {
        log.info("We can make a trade! Net profit would be ${possibleEarning.asEth()}")

        log.info("user: $user")
        log.info("Going to use this collateral")
        log.info(
            "${result.second.asset.address} ${result.second.asset.name} (${result.second.valueInEth.asEth()} ETH) (${result.second.asset.liquidationBonus}%)"
        )

        log.info("Best DebtToPay")
        log.info(
            "${result.first.asset.address} ${result.first.asset.name} (${
                result.first.liquidatableInEth.asEth()
            } ETH), possible earnings: ${possibleEarning.asEth()}"
        )
        log.info(
            "This requires us to have around ${
                FormatUtils.asEth(result.first.liquidatableDebt, result.first.asset.decimals)
            } ${result.first.asset.name}"
        )
    }

    private fun userReservesSortedByValue(userReserveData: List<UserReserveData>) = getReserves(userReserveData)
        .sortedByDescending(Collateral::valueInEth)

    fun getDebts(userReserveData: List<UserReserveData>): List<Debt> {
        return userReserveData
            .filter {
                it.currentVariableDebt > BigInteger.ZERO || it.currentStableDebt > BigInteger.ZERO
            }.map {
                val totalAmount = it.currentVariableDebt + it.currentStableDebt
                val liquidatableDebt = totalAmount.divide(BigInteger.valueOf(2))
                val price = aaveService.oracleContract.getPrice(it.asset.address)
                Debt(
                    asset = it.asset,
                    totalAmount = totalAmount,
                    assetPrice = price,
                    liquidatableInEth = price.multiply(liquidatableDebt)
                        .divide(BigInteger.TEN.pow(it.asset.decimals)),
                    liquidatableDebt = liquidatableDebt
                )
            }
    }

    fun getReserves(userReserveData: List<UserReserveData>): List<Collateral> {
        return userReserveData.filter {
            it.usageAsCollateralEnabled
        }.filter {
            it.currentATokenBalance > BigInteger.ZERO
        }.map {
            val price = aaveService.oracleContract.getPrice(it.asset.address)
            Collateral(
                asset = it.asset,
                totalAmount = it.currentATokenBalance,
                valueInEth = price.multiply(it.currentATokenBalance)
                    .divide(BigInteger.TEN.pow(it.asset.decimals)),
                assetPrice = price
            )
        }.toList()
    }
}