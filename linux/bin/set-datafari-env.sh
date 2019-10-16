#!/bin/bash -e
#
#
# Set Datafari environment variable
#
#

export DATAFARI_USER="datafari"
export POSTGRES_USER="postgres"

export DATAFARI_HOME=$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )
#export JAVA_HOME=${DATAFARI_HOME}/jvm
export JAVA_OPTS="${JAVA_OPTS} -Duser.timezone=UTC"
export PATH=${PATH}:$JAVA_HOME/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
export INIT_STATE_FILE=${DATAFARI_HOME}/bin/common/init_state.properties
export CONFIG_FILE=${DATAFARI_HOME}/tomcat/conf/datafari.properties
export LOG4J_VERSION=2.12.0
export SLF4J_VERSION=1.7.25
export COMMONS_LOGGING_VERSION=1.2
export LD_LIBRARY_PATH=${DATAFARI_HOME}/pgsql/lib

export PID_DIR=$DATAFARI_HOME/pid
export TOMCAT_HOME=$DATAFARI_HOME/tomcat
export TOMCAT_MCF_HOME=$DATAFARI_HOME/tomcat-mcf
export MCF_HOME=$DATAFARI_HOME/mcf/mcf_home
export SOLR_INSTALL_DIR=$DATAFARI_HOME/solr
export SOLR_ENV=$SOLR_INSTALL_DIR/bin/solr.in.sh
export CASSANDRA_HOME=$DATAFARI_HOME/cassandra
export CASSANDRA_ENV=$CASSANDRA_HOME/bin/cassandra.in.sh
export CASSANDRA_INCLUDE=$CASSANDRA_ENV
export CASSANDRA_HOST=127.0.0.1
export CASSANDRA_PORT=9042
export ZK_HOME=$DATAFARI_HOME/zookeeper
export ZK_ENV=$ZK_HOME/bin/zkEnvh.sh
export ELK_HOME=$DATAFARI_HOME/elk
export ELASTICSEARCH_HOME=$DATAFARI_HOME/elk/elasticsearch
export LOGSTASH_HOME=$ELK_HOME/logstash
export KIBANA_HOME=$DATAFARI_HOME/elk/kibana

# Logs directory: needed for Cassandra startup
export DATAFARI_LOGS=$DATAFARI_HOME/logs

# Configs
export CONFIG_HOME=$TOMCAT_HOME/conf
export MAIN_DATAFARI_CONFIG_HOME=$TOMCAT_HOME/conf
export DATAFARI_SOLR_PROPERTIES_HOME=$TOMCAT_HOME/conf


# pid files 
export MCF_PID_FILE=$PID_DIR/mcf_crawler_agent.pid
export CATALINA_PID=$PID_DIR/tomcat.pid
export CATALINA_MCF_PID=$PID_DIR/tomcat-mcf.pid
export SOLR_PID_FILE=$PID_DIR/solr-8983.pid
export CASSANDRA_PID_FILE=$PID_DIR/cassandra.pid
export POSTGRES_PID_FILE=$PID_DIR/postmaster.pid
export ZK_PID_FILE=$PID_DIR/zookeeper_server.pid
export ELASTICSEARCH_PID_FILE=$PID_DIR/elasticsearch.pid
export KIBANA_PID_FILE=$PID_DIR/kibana.pid

export RETRIES_NUMBER=35