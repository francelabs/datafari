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
source $AS_HOME/scripts/set-as-env.sh



if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $SOLR_PID_FILE"; then
  run_as ${DATAFARI_USER} "bash datafari-manager.sh stop_solr";
else
  echo "Warn : Solr does not seem to be running."
fi


if  [[ "$AnalyticsActivation" = *true* ]]; then
  
  cd $AS_HOME/scripts
  run_as ${DATAFARI_USER} "bash as-manager.sh stop_logstash";
  cd $DIR
    
     
fi

if [ "$(whoami)" == "root" ]; then
  bash ${DIR}/datafari-manager.sh stop_apache
else
  run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh stop_apache"
fi
