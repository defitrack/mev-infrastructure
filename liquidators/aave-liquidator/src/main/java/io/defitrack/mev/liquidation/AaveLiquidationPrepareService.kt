package io.defitrack.mev.liquidation

import io.defitrack.mev.AaveLiquidationCallService
import io.defitrack.mev.chains.polygon.config.PolygonContractAccessor
import io.defitrack.mev.common.FormatUtils
import io.defitrack.mev.common.FormatUtilsExtensions.asEth
import io.defitrack.mev.liquidationcall.AaveLiquidationCall
import io.defitrack.mev.protocols.aave.AaveService
import io.defitrack.mev.protocols.aave.ReserveToken
import io.defitrack.mev.protocols.aave.UserAccountData
import io.defitrack.mev.protocols.aave.UserReserveData
import io.defitrack.mev.user.UserService
import io.defitrack.mev.user.domain.AaveUser
import org.bouncycastle.util.encoders.Hex
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.core.DefaultBlockParameterName
import java.math.BigInteger

@Component
class AaveLiquidationPrepareService(
    private val aaveLiquidationCallService: AaveLiquidationCallService,
    private val polygonContractAccessor: PolygonContractAccessor,
    private val userService: UserService,
    private val aaveService: AaveService,
    @Value("\${io.defitrack.liquidator.private-key}") private val whitehatPriveKey: String,
    @Value("\${io.defitrack.liquidator.address}") private val whitehatAddress: String,
    @Value("\${io.defitrack.liquidator.contract-address}") private val liquidatorContractAddress: String
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }


    private val liquidationGasLimit = BigInteger.valueOf(2500000)

    private val protocolDataProviderContract = aaveService.getLendingPoolDataProviderContract()

    fun prepare(user: AaveUser) {
        val allUserReserves = userReservesSortedByValue(user)
        val allUserDebts = getDebtsForUser(user.address)

        val result = getBestDebtAndCollateral(allUserDebts, allUserReserves)

        val liquidationBonusInEth = getLiquidationBonusInEth(result)
        val actualProfit = liquidationBonusInEth.asEth()
        val healthFactor = getHealthFactor(user)
        if (actualProfit > 0 && healthFactor < 1.0) {
            logLiquidationPossibility(result, liquidationBonusInEth, user)
            liquidate(
                result.second.asset.address,
                result.first.asset.address,
                user.address,
                result.first.liquidatableDebt,
                result.first
            )
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

    private fun liquidate(
        collateralAddress: String,
        assetToRepay: String,
        user: String,
        debtToCover: BigInteger,
        debt: Debt
    ): String {
        log.info(
            """
            We can liquidate: 
            collateral: $collateralAddress
            asset to repay: $assetToRepay
            user: $user,
            debt to cover: $debtToCover
            debt: $debt
        """.trimIndent()
        )

        return ""
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
            calculateTotalPossibleLiquidatableInEth(result.first!!, result.second!!)
                .multiply(BigInteger.valueOf(result.second!!.asset.liquidationBonus.toLong()))
                .divide(
                    BigInteger.valueOf(10000)
                )
        }
    }

    private fun getBestDebtAndCollateral(
        allUserDebts: List<Debt>,
        allUserReserves: List<Collateral>
    ) = allUserDebts
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
        polygonContractAccessor.polygonGateway.web3j()
            .ethGetTransactionCount(whitehatAddress, DefaultBlockParameterName.LATEST)
            .send().transactionCount,
        BigInteger.valueOf(60000000000),
        liquidationGasLimit,
        liquidatorContractAddress,
        BigInteger.ZERO,
        liquidate
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


    private fun getUserReserveData(
        asset: ReserveToken, user: String,
    ): UserReserveData? {
        return try {
            protocolDataProviderContract.getReserveData(user, asset)
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    private fun userReservesSortedByValue(user: AaveUser) = getReservesForUser(user.address)
        .sortedByDescending(Collateral::valueInEth)

    fun getDebtsForUser(user: String) = protocolDataProviderContract.allReservesTokens.mapNotNull {
        getUserReserveData(it, user)
    }
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

    fun getReservesForUser(user: String) = protocolDataProviderContract.allReservesTokens.asSequence()
        .mapNotNull {
            getUserReserveData(it, user)
        }.filter {
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