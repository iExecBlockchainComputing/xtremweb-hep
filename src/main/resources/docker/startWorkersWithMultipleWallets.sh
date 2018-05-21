#!/bin/bash


# read a file called wallets.list line by line then call the script startWorkerWithSingleWallet.sh
# the wallets.list file is made up lines as follow:
# WALLET_PATH WALLET_PASSWORD

NB_CORES=`nproc --all`
MAX_NB_WORKERS=$(($NB_CORES - 1))
COUNTER=0

while IFS='' read -r line || [[ -n "$line" ]]; do
    COUNTER=$[$COUNTER +1]
    if [ $COUNTER -gt $MAX_NB_WORKERS ]; then
      break
    fi

    WALLET_PATH=`echo $line | awk -F ' ' '{print $1}'`
    WALLET_PASSWORD=`echo $line | awk -F ' ' '{print $2}'`
    WORKER_NAME=worker_$COUNTER

    ./startWorkerWithSingleWallet.sh $WORKER_NAME $WALLET_PATH $WALLET_PASSWORD
done < wallets.list
