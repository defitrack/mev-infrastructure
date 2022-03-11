const { expect } = require("chai");
const { ethers } = require("hardhat");
const {BigNumber} = require("ethers");

describe("AaveLiquidator", function () {

    it("should be able to liquidate this specific user", async function () {
        const liquidatableBlock = 25818026

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
            "0x9a71012B13CA4d3D0Cdc72A177DF3ef03b0E76A3",
            "0x7ceB23fD6bC0adD59E62ac25578270cFf1b9f619",
            "0x80AB6dc6e6Ca99f2319213cd97fa02c14E4bd434", //user
            BigNumber.from("6254124772914618898") //amount
        );
    });
});
