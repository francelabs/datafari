#!/bin/bash -e
#
#
# Start ZK only
#
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/set-datafari-env.sh"
source "${DIR}/utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE


if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $ZK_PID_FILE"; then
   PID=$(run_as ${DATAFARI_USER} "cat $ZK_PID_FILE");
   echo "Error : Zookeeper seems to be already running with PID $PID"
   exit 1
fi

if  [[ "$NODETYPE" = *solr* ]]
then
  run_as ${DATAFARI_USER} "bash datafari-manager.sh start_zookeeper";
fi
