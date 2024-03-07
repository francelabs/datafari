#!/bin/bash -e
#
#
# Start Solr only
#
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/set-datafari-env.sh"
source "${DIR}/utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE
source $AS_HOME/scripts/set-as-env.sh


if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $SOLR_PID_FILE"; then
  PID=$(run_as ${DATAFARI_USER} "cat $SOLR_PID_FILE");
  echo "Error : Solr seems to be already running with PID $PID"
  exit 1
fi

if  [[ "$NODETYPE" = *solr* ]];
then
  
    
  run_as ${DATAFARI_USER} "bash datafari-manager.sh start_solr";



  if  [[ "$AnalyticsActivation" = *true* ]]; then
    if  [[ "$STATE" = *initialized* ]]; then
      cd $AS_HOME/scripts
      run_as ${DATAFARI_USER} "bash as-manager.sh init_logstash";
      cd $DIR
        
    fi
    
    cd $AS_HOME/scripts
    run_as ${DATAFARI_USER} "bash as-manager.sh start_logstash";
    cd $DIR
    
        
    
  fi
  
  if [ "$(whoami)" == "root" ]; then
    bash ${DIR}/datafari-manager.sh start_apache
  else
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh start_apache"
  fi

  if  [[ "$STATE" = *initialized* ]]; then
    run_as ${DATAFARI_USER} `sed -i 's/\(STATE *= *\).*/\1active/' $INIT_STATE_FILE`  
  fi
fi
