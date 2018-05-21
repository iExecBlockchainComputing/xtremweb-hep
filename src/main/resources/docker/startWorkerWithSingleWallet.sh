#!/bin/bash


# get parameters from the command line
if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]
  then
    echo "Please call this script with the arguments WORKER_NAME WALLET_PATH and WALLET_PASSWORD."
		exit 1
fi

WORKER_NAME=$1
WALLET_PATH=$2
WALLET_PASSWORD=$3

################################################################################
# in the .env file, variables are stored like this: VAR_NAME=VALUE
# This method will return VALUE given the VAR_NAME
################################################################################
function getValueFromEnvFile() {
	echo `grep $1 .env | tr = ' ' | awk -F ' ' '{print $2}'`
}

################################################################################
# find an availabe name of temporary folder for the worker on the host machine
################################################################################
function getResultsFolderName() {
	FOLDER_NAME="/tmp/worker"
	RESULTS_FOLDER=""
	CAN_CREATE_NEW_FOLDER=false
	NUMBER_MAX_TRY=100

	for i in  $(eval echo "{1..$NUMBER_MAX_TRY}")
	do
	  if [ ! -d "$FOLDER_NAME$i" ]; then
		RESULTS_FOLDER="$FOLDER_NAME$i"
		CAN_CREATE_NEW_FOLDER=true
		break
	  fi
	done

	if [ "$CAN_CREATE_NEW_FOLDER" == false ]; then
	  echo "There are more than $NUMBER_MAX_TRY folders /tmp/worker# on the machine, please delete them if they are unused."
	  exit 1
	fi

	echo $RESULTS_FOLDER
}

########################################################

docker run -d --restart unless-stopped \
							--env SCHEDULER_IP=scheduler \
							--env SCHEDULER_DOMAIN=iexecscheduler \
							--env TMPDIR=$RESULT_FOLDER_NAME \
							--env SANDBOXENABLED=$(getValueFromEnvFile WORKER_SANDBOX_ENABLED) \
							--env WALLETPATH=$WALLET_PATH \
							--env WALLETPASSWORD=$WALLET_PASSWORD \
							--env LOGGERLEVEL=$(getValueFromEnvFile LOGGERLEVEL) \
							-v /var/run/docker.sock:/var/run/docker.sock
							-v $RESULT_FOLDER_NAME:$RESULT_FOLDER_NAME
							-v $WALLET_PATH:/iexec/wallet/wallet.json
							--network=docker_iexec-net
							--name $WORKER_NAME
							iexechub/worker:$(getValueFromEnvFile "WORKER_DOCKER_IMAGE_VERSION")


# ports to expose?
#		- 4321
#		- 4322
#		- 4323
#		- 443
