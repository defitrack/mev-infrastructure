const { expect } = require("chai");
const { ethers } = require("hardhat");
const {BigNumber} = require("ethers");

describe("AaveLiquidator", function () {

    it("should be able to liquidate this specific user", async function () {
        const liquidatableBlock = 25651219

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
            "0x385Eeac5cB85A38A9a07A70c73e0a3271CfB54A7", //ghst
            "0x0d500B1d8E8eF31E21C99d1Db9A6444d3ADf1270", //wmatic
            "0xDAb42a3D1932D720BE40CecC97Dcd200317Ee8aC", //user
            BigNumber.from("13534813803504476") //amount
        );

        console.log('done');
    });
});
