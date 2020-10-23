#!/bin/bash -e
#
#
# Shutdown script for Solr only
#
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/set-datafari-env.sh"
source "${DIR}/utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE
source "${DIR}/../elk/scripts/set-elk-env.sh"
source "${DIR}/../elk/scripts/elk-utils.sh"



if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $SOLR_PID_FILE"; then
   run_as ${DATAFARI_USER} "bash datafari-manager.sh stop_solr";
else
   echo "Warn : Solr does not seem to be running."
fi


if  [[ "$ELKactivation" = *true* ]]; then
	if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $LOGSTASH_PID_FILE"; then
		cd $ELK_HOME/scripts
      	run_as ${DATAFARI_USER} "bash elk-manager.sh stop_logstash";
      	cd $DIR
    else
    	echo "Error : Logstash does not seem to be running."
    	
    fi
    
fi