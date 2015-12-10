#!/bin/bash -e
#
#
# Shutdown script for Datafari
#
#

if (( EUID != 0 )); then
   echo "You need to be root to run this script." 1>&2
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


if is_running $MCF_PID_FILE; then
    cd $MCF_HOME/../bin
    bash mcf_crawler_agent.sh stop
    forceStopIfNecessary $MCF_PID_FILE McfCrawlerAgent
else
    echo "Warn: MCF Agent does not seem to be running."
fi

if is_running $CATALINA_PID; then
    cd $TOMCAT_HOME
    waitTomcat
    bash bin/shutdown.sh 30
    forceStopIfNecessary $CATALINA_PID Tomcat
else
    echo "Warn: Tomcat does not seem to be running."
fi

if is_running $SOLR_PID_FILE; then
   SOLR_INCLUDE=$SOLR_ENV $SOLR_INSTALL_DIR/bin/solr stop
else
   echo "Warn : Solr does not seem to be running."
fi

if is_running $CASSANDRA_PID_FILE; then
   kill $(cat $CASSANDRA_PID_FILE)
   rm $CASSANDRA_PID_FILE
else
   echo "Warn : Cassandra does not seem to be running."
fi

if is_running $POSTGRES_PID_FILE; then
	su postgres -c "${DATAFARI_HOME}/pgsql/bin/pg_ctl -D ${DATAFARI_HOME}/pgsql/data -l ${DATAFARI_HOME}/logs/pgsql.log stop"
else
   echo "Warn : Postgres does not seem to be running."
fi

if is_running $KIBANA_PID_FILE; then
   kill $(cat $KIBANA_PID_FILE)
   rm $KIBANA_PID_FILE
else
   echo "Warn : Kibana does not seem to be running."
fi

if is_running $LOGSTASH_PID_FILE; then
   kill $(cat $LOGSTASH_PID_FILE)
   rm $LOGSTASH_PID_FILE
else
   echo "Warn : Logstash does not seem to be running."
fi

if is_running $ELASTICSEARCH_PID_FILE; then
   kill $(cat $ELASTICSEARCH_PID_FILE)
   rm $ELASTICSEARCH_PID_FILE
else
   echo "Warn : Elasticsearch does not seem to be running."
fi


