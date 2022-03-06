require("dotenv").config();

require("@nomiclabs/hardhat-etherscan");
require("@nomiclabs/hardhat-waffle");
require("hardhat-gas-reporter");
require("solidity-coverage");


// This is a sample Hardhat task. To learn how to create your own go to
// https://hardhat.org/guides/create-task.html
task("accounts", "Prints the list of accounts", async (taskArgs, hre) => {
  const accounts = await hre.ethers.getSigners();

  for (const account of accounts) {
    console.log(account.address);
  }
});

// You need to export an object to set up your config
// Go to https://hardhat.org/config/ to learn more

/**
 * @type import('hardhat/config').HardhatUserConfig
 */
module.exports = {
  solidity: "0.8.4",
  networks: {
    hardhat: {

    },
    polygon: {
      gas: 2100000,
      gasPrice: 8000000000,
      saveDeployments: true,
      url: "https://polygon-mainnet.g.alchemy.com/v2/l3Y1naqLwO2GmYqyR6Q7a6UR_2KwSHoQ",
      chainId: 137,
      accounts:
          process.env.PRIVATE_KEY !== undefined ? [process.env.PRIVATE_KEY] : [],
    },
  },
  gasReporter: {
    enabled: process.env.REPORT_GAS !== undefined,
    currency: "USD",
  },
  etherscan: {
    apiKey: {
      polygon: process.env.ETHERSCAN_API_KEY_POLYGON,
      opera: process.env.ETHERSCAN_API_KEY_FTM
    },
  },
};
