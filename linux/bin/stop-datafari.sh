#!/bin/bash -e
#
#
# Shutdown script for Datafari
#
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/set-datafari-env.sh"
source "${DIR}/utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE
source $AS_HOME/scripts/set-as-env.sh
source $TIKA_SERVER_HOME/bin/set-tika-env.sh

if [ "$MONIT_STATE" == "active" ]; then
        sudo service monit stop
fi

if run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh is_running $CATALINA_PID"; then
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh stop_tomcat"
else
    echo "Warn: Tomcat does not seem to be running."
fi

if run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh is_running $CATALINA_MCF_PID"; then
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh stop_tomcat_mcf"
else
    echo "Warn: Tomcat-MCF does not seem to be running."
fi


if run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh is_running $MCF_PID_FILE"; then
    run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh stop_mcf_crawler_agent";
else
    echo "Warn: MCF Agent does not seem to be running."
fi

if run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh is_running $SOLR_PID_FILE"; then
   run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh stop_solr";
else
   echo "Warn : Solr does not seem to be running."
fi

if run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh is_running $ZK_PID_FILE"; then
   run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh stop_zookeeper";
else
   echo "Warn : Zookeeper does not seem to be running."
fi

if run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh is_running $ZK_MCF_PID_FILE"; then
   run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh stop_zookeeper_mcf";
else
   echo "Warn : Zookeeper MCF does not seem to be running."
fi

if run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh is_running $CASSANDRA_PID_FILE"; then
   run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh stop_cassandra";
else
   echo "Warn : Cassandra does not seem to be running."
fi


if run_as ${POSTGRES_USER} "bash ${DIR}/datafari-manager.sh is_running $POSTGRES_PID_FILE"; then
  run_as ${POSTGRES_USER} "bash ${DIR}/datafari-manager.sh stop_postgres"
else
   echo "Warn : Postgres does not seem to be running."
fi

if [ "$(whoami)" == "root" ]; then
  bash ${DIR}/datafari-manager.sh stop_apache
else
  run_as ${DATAFARI_USER} "bash ${DIR}/datafari-manager.sh stop_apache"
fi

if  [[ "$TIKASERVER" = *true* ]];
then
  cd $TIKA_SERVER_HOME/bin
  run_as ${DATAFARI_USER} "bash tika-server.sh stop"
  cd $DIR
fi

 
  if  [[ "$TIKASERVER_ANNOTATOR" = *true* ]];
  then
    cd $TIKA_SERVER_HOME_ANNOTATOR/bin
    run_as ${DATAFARI_USER} "bash tika-server.sh stop";
    cd $DIR
  fi
  
  if  [[ "$TIKASERVER_OCR" = *true* ]];
  then
    cd $TIKA_SERVER_HOME_OCR/bin
    run_as ${DATAFARI_USER} "bash tika-server.sh stop";
    cd $DIR
  fi

if  [[ "$AnalyticsActivation" = *true* ]]; then
  #Stop Analytic Stack
  cd $AS_HOME/scripts
  run_as ${DATAFARI_USER} "bash as-manager.sh stop";
  cd $DIR
fi