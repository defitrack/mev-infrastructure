const { expect } = require("chai");
const { ethers } = require("hardhat");
const {BigNumber} = require("ethers");

describe("AaveLiquidator", function () {

    it("should be able to liquidate this specific user", async function () {
        const liquidatableBlock = 25862040

        await network.provider.request({
            method: "hardhat_reset",
            params: [
                {
                    forking: {
                        jsonRpcUrl: "https://polygon-mainnet.g.alchemy.com/v2/l3Y1naqLwO2GmYqyR6Q7a6UR_2KwSHoQ",
                        blockNumber: liquidatableBlock,
                    },
                },
            ],
        });
        const Liquidator = await ethers.getContractFactory("AaveLiquidator");
        const liquidator = await Liquidator.deploy("0xd05e3E715d945B59290df0ae8eF85c1BdB684744");
        await liquidator.deployed();

        console.log('liquidating')
        await liquidator.liquidate(
            "0x8f3Cf7ad23Cd3CaDbD9735AFf958023239c6A063",
            "0x8f3Cf7ad23Cd3CaDbD9735AFf958023239c6A063",
            "0x598778c0d3ec94FE8fd2519F7a386D8b6A132058", //user
            BigNumber.from("10073813707727147706") //amount
        );
    });
});
