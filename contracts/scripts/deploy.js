const hre = require("hardhat");

async function main() {

    let aaveAddressesProvider = "0xd05e3E715d945B59290df0ae8eF85c1BdB684744";
    await hre.run('compile');

    // We get the contract to deploy
    const AaveLiquidator = await hre.ethers.getContractFactory("AaveLiquidator");
    const aaveLiquidator = await AaveLiquidator.deploy(aaveAddressesProvider);

    await aaveLiquidator.deployed();

    console.log("aaveLiquidator deployed to:", aaveLiquidator.address);

    await delay(10000)

    await hre.run("verify:verify", {
        address: aaveLiquidator.address,
        constructorArguments: [
            aaveAddressesProvider
        ],
    })
}

// We recommend this pattern to be able to use async/await everywhere
// and properly handle errors.
main().catch((error) => {
    console.error(error);
    process.exitCode = 1;
});

const delay = ms => new Promise(res => setTimeout(res, ms));
