#!/bin/bash

docker build -t defitrack/defitrack:mev-aave-liquidator-${BRANCH_NAME} defitrack-rest/defitrack-protocol-services/defitrack-uniswap
