#!/bin/bash -e
#
#
# Shutdown script for Datafari
#
#

if (( EUID == 0 )); then
   echo "You need to be a non-root user to run this script." 1>&2
   exit 100
fi

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/set-datafari-env.sh"
source "${DIR}/utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE


if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $MCF_PID_FILE"; then
    run_as ${DATAFARI_USER} "bash datafari-manager.sh stop_mcf_crawler_agent";
else
    echo "Warn: MCF Agent does not seem to be running."
fi

if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $CATALINA_PID"; then
    run_as ${DATAFARI_USER} "bash datafari-manager.sh stop_tomcat"
else
    echo "Warn: Tomcat does not seem to be running."
fi

if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $CATALINA_MCF_PID"; then
    run_as ${DATAFARI_USER} "bash datafari-manager.sh stop_tomcat_mcf"
else
    echo "Warn: Tomcat-MCF does not seem to be running."
fi


if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $SOLR_PID_FILE"; then
   run_as ${DATAFARI_USER} "bash datafari-manager.sh stop_solr";
else
   echo "Warn : Solr does not seem to be running."
fi

if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $ZK_PID_FILE"; then
   run_as ${DATAFARI_USER} "bash datafari-manager.sh stop_zookeeper";
else
   echo "Warn : Zookeeper does not seem to be running."
fi

if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $CASSANDRA_PID_FILE"; then
   run_as ${DATAFARI_USER} "bash datafari-manager.sh stop_cassandra";
else
   echo "Warn : Cassandra does not seem to be running."
fi

if run_as ${DATAFARI_USER} "bash datafari-manager.sh is_running $POSTGRES_PID_FILE"; then
  run_as ${POSTGRES_USER} "bash datafari-manager.sh stop_postgres"
else
   echo "Warn : Postgres does not seem to be running."
fi
