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


isMCFRunning=true
isTomcatRunning=true
isSolrRunning=true
isCassandraRunning=true
isZKRunning=true

if is_running $MCF_PID_FILE; then
    cd $MCF_HOME/../bin
    run_as ${DATAFARI_USER} "export PATH=$PATH && bash mcf_crawler_agent.sh stop"
    forceStopIfNecessary $MCF_PID_FILE McfCrawlerAgent
else
    echo "Warn: MCF Agent does not seem to be running."
fi

if is_running $CATALINA_PID; then
    cd $TOMCAT_HOME
    waitTomcat
    run_as ${DATAFARI_USER} "bash bin/shutdown.sh 30"
    forceStopIfNecessary $CATALINA_PID Tomcat
else
    echo "Warn: Tomcat does not seem to be running."
fi


if is_running $SOLR_PID_FILE; then
   run_as ${DATAFARI_USER} "SOLR_INCLUDE=$SOLR_ENV $SOLR_INSTALL_DIR/bin/solr stop"
else
   echo "Warn : Solr does not seem to be running."
fi

if is_running $ZK_PID_FILE; then
   run_as ${DATAFARI_USER} "bash $ZK_HOME/bin/zkServer.sh stop"
else
   echo "Warn : Zookeeper does not seem to be running."
fi

if is_running $CASSANDRA_PID_FILE; then
   run_as ${DATAFARI_USER} "kill $(cat $CASSANDRA_PID_FILE)"
   run_as ${DATAFARI_USER} "rm -f $CASSANDRA_PID_FILE"
else
   echo "Warn : Cassandra does not seem to be running."
fi

if is_running $POSTGRES_PID_FILE; then
	sudo LD_LIBRARY_PATH=${DATAFARI_HOME}/pgsql/lib su postgres -p -c "${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_HOME}/logs/pgsql.log stop"
else
   echo "Warn : Postgres does not seem to be running."
fi
