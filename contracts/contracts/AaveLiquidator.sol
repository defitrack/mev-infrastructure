//SPDX-License-Identifier: Unlicense

pragma solidity ^0.8.0;

import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@openzeppelin/contracts/token/ERC20/utils/SafeERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "./aave/ILendingPoolAddressesProvider.sol";
import "./aave/ILendingPool.sol";
import "./aave/IFlashLoanReceiver.sol";
import "./uniswap/IUniswapV2Router02.sol";
import "./uniswap/IUniswapV2Factory.sol";
import "./balancer/IBalancerVault.sol";

contract AaveLiquidator is IFlashLoanReceiver, Ownable {

    address public uniswapRouter02Address = 0xa5E0829CaCEd8fFDD4De3c43696c57F7D7A678ff;
    address public uniswapFactoryAddress = 0x5757371414417b8C6CAad45bAeF941aBc7d3Ab32;
    address public balancerVault = 0xBA12222222228d8Ba445958a75a0704d566BF2C8;
    address public weth = 0x7ceB23fD6bC0adD59E62ac25578270cFf1b9f619;
    address public bal = 0x9a71012B13CA4d3D0Cdc72A177DF3ef03b0E76A3;
    address public usdc = 0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174;
    address public wmatic = 0x0d500B1d8E8eF31E21C99d1Db9A6444d3ADf1270;

    address internal constant ETHAddress =
    0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE;

    uint256 private constant deadline = 0xf000000000000000000000000000000000000000000000000000000000000000;

    IBalancerVault.SwapKind public swapKind;
    IBalancerVault.FundManagement public funds;

    ILendingPoolAddressesProvider public immutable ADDRESSES_PROVIDER;
    ILendingPool public immutable LENDING_POOL;



    using SafeERC20 for IERC20;

    constructor(ILendingPoolAddressesProvider _addressProvider) public {
        ADDRESSES_PROVIDER = _addressProvider;
        LENDING_POOL = ILendingPool(_addressProvider.getLendingPool());
        funds = IBalancerVault.FundManagement(address(this), false, payable(address(this)), false);
    }

    struct LiquidationData {
        address collateral;
        address debt;
        address user;
        uint256 debtToPay;
    }

    /**
        This function is called after your contract has received the flash loaned amount
     */
    function executeOperation(
        address[] calldata assets,
        uint256[] calldata amounts,
        uint256[] calldata premiums,
        address initiator,
        bytes calldata params
    )
    external
    override
    returns (bool)
    {

        LiquidationData memory liquidationData = abi.decode(params, (LiquidationData));

        performLiquidation(liquidationData.collateral, liquidationData.debt, liquidationData.user, amounts[0]);

        uint256 receivedCollateral = IERC20(liquidationData.collateral).balanceOf(address(this));

        swap(liquidationData.collateral, liquidationData.debt, receivedCollateral);
        uint256 resultingBalance = IERC20(liquidationData.debt).balanceOf(address(this));

        //required to repay the flash loan
        uint amountOwing = amounts[0] + premiums[0];

        require(amountOwing < resultingBalance, "didnt't receive enough debt tokens to repay");

        safeAllow(assets[0], address(LENDING_POOL));
        return true;
    }

    function swap(address _FromTokenContractAddress,
        address _ToTokenContractAddress,
        uint256 tokens2Trade) internal {
        if(_FromTokenContractAddress == bal || _ToTokenContractAddress == bal) {
            if(_ToTokenContractAddress == weth || _FromTokenContractAddress == weth || _ToTokenContractAddress == usdc || _FromTokenContractAddress == usdc || _ToTokenContractAddress == wmatic || _FromTokenContractAddress == wmatic) {
                //do bal -> weth
                bytes32 poolId = 0x0297e37f1873d2dab4487aa67cd56b58e2f27875000100000000000000000002;
                balancerSwap(poolId, _FromTokenContractAddress, _ToTokenContractAddress, tokens2Trade);
            } else {
                revert("no correct balancer path implemented");
            }
        } else {
            quickswap(_FromTokenContractAddress, _ToTokenContractAddress, tokens2Trade);
        }
    }

    function balancerSwap(bytes32 _poolId, address _tokenIn, address _tokenOut, uint256 _amountIn) internal returns (uint256) {
        safeAllow(
            _tokenIn,
            balancerVault
        );
        IBalancerVault.SingleSwap memory singleSwap = IBalancerVault.SingleSwap(_poolId, swapKind, _tokenIn, _tokenOut, _amountIn, "");
        return IBalancerVault(balancerVault).swap(singleSwap, funds, 1, block.timestamp);
    }

    function performLiquidation(
        address _collateral,
        address _debt,
        address _user,
        uint256 _debtToPay
    ) private {
        safeAllow(_debt, address(LENDING_POOL));
        LENDING_POOL.liquidationCall(_collateral, _debt, _user, _debtToPay, false);
    }

    function liquidate(
        address _debt,
        address _collateral,
        address _user,
        uint256 _debtToPay
    ) public onlyOwner {

        require(unhealthyUser(_user), 'user was healthy');

        address receiverAddress = address(this);

        address[] memory assets = new address[](1);
        assets[0] = address(_debt);

        uint256[] memory amounts = new uint256[](1);
        amounts[0] = _debtToPay;

        // 0 = no debt, 1 = stable, 2 = variable
        uint256[] memory modes = new uint256[](1);
        modes[0] = 0;

        address onBehalfOf = address(this);
        bytes memory params = abi.encode(LiquidationData({
        collateral : _collateral,
        user : _user,
        debt : _debt,
        debtToPay : _debtToPay
        }));
        uint16 referralCode = 0;

        LENDING_POOL.flashLoan(
            receiverAddress,
            assets,
            amounts,
            modes,
            onBehalfOf,
            params,
            referralCode
        );

        uint256 resultingDebtTokens = IERC20(_debt).balanceOf(address(this));
        if (resultingDebtTokens > 0) {
            IERC20(_debt).transfer(owner(), resultingDebtTokens);
        }
        uint256 resultingCollateralTokens = IERC20(_collateral).balanceOf(address(this));
        if (resultingCollateralTokens > 0) {
            IERC20(_collateral).transfer(owner(), resultingCollateralTokens);
        }
    }


    function safeAllow(address asset, address allowee) private {
        IERC20 token = IERC20(asset);

        if (token.allowance(address(this), allowee) == 0) {
            token.safeApprove(allowee, type(uint256).max);
        }
    }


    function quickswap(
        address _FromTokenContractAddress,
        address _ToTokenContractAddress,
        uint256 tokens2Trade
    ) internal returns (uint256 tokenBought) {
        if (_FromTokenContractAddress == _ToTokenContractAddress) {
            return tokens2Trade;
        }

        safeAllow(
            _FromTokenContractAddress,
            uniswapRouter02Address
        );

        address pair =
        IUniswapV2Factory(uniswapFactoryAddress).getPair(
            _FromTokenContractAddress,
            _ToTokenContractAddress
        );

        if (pair != address(0)) {
            address[] memory path = new address[](2);
            path[0] = _FromTokenContractAddress;
            path[1] = _ToTokenContractAddress;

            tokenBought = IUniswapV2Router02(uniswapRouter02Address).swapExactTokensForTokens(
                tokens2Trade,
                1,
                path,
                address(this),
                deadline
            )[path.length - 1];

            require(tokenBought > 0, "Error Swapping Tokens 2");
        } else {
            address[] memory path = new address[](3);
            path[0] = _FromTokenContractAddress;
            path[1] = weth;
            path[2] = _ToTokenContractAddress;

            tokenBought = IUniswapV2Router02(uniswapRouter02Address).swapExactTokensForTokens(
                tokens2Trade,
                1,
                path,
                address(this),
                deadline
            )[path.length - 1];

            require(tokenBought > 0, "Error Swapping Tokens 3");
        }
    }

    function unhealthyUser(address _user) public view returns (bool){
        (uint totalCollateralETH, uint totalDebtEth, uint availableBorrowsETH,  uint currentLiquidationThreshold, uint ltv, uint hf) = LENDING_POOL.getUserAccountData(_user);
        return hf < 1 ether;
    }

    function withdrawTokens(address[] calldata tokens) external onlyOwner {
        for (uint256 i = 0; i < tokens.length; i++) {
            uint256 qty;

            if (tokens[i] == ETHAddress) {
                qty = address(this).balance;
                Address.sendValue(payable(owner()), qty);
            } else {
                qty = IERC20(tokens[i]).balanceOf(address(this));
                IERC20(tokens[i]).safeTransfer(owner(), qty);
            }
        }
    }
}