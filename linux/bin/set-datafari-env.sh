#!/bin/bash -e
#
#
# Set Datafari environment variable
#
#

export DATAFARI_USER="datafari"
export POSTGRES_USER="postgres"

export DATAFARI_HOME=$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )
export PATH=${PATH}:$JAVA_HOME/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
export TRUSTSTORE_PATH=${DATAFARI_HOME}/ssl-keystore/datafari-truststore.p12
export TRUSTSTORE_PASSWORD=DataFariAdmin
export JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.trustStore=${TRUSTSTORE_PATH} -Djavax.net.ssl.trustStorePassword=${TRUSTSTORE_PASSWORD} -Duser.timezone=UTC"
export INIT_STATE_FILE=${DATAFARI_HOME}/bin/common/init_state.properties
export CONFIG_FILE=${DATAFARI_HOME}/tomcat/conf/datafari.properties
export SOLR_CONFIG_FILE=${DATAFARI_HOME}/tomcat/conf/solr.properties
export LOG4J_VERSION=2.17.1
export LD_LIBRARY_PATH=${DATAFARI_HOME}/pgsql/lib
export TMP_DIR=/tmp


# Home dirs
export PID_DIR=$DATAFARI_HOME/pid
export TOMCAT_HOME=$DATAFARI_HOME/tomcat
export TOMCAT_MCF_HOME=$DATAFARI_HOME/tomcat-mcf
export MCF_HOME=$DATAFARI_HOME/mcf/mcf_home
export SOLR_INSTALL_DIR=$DATAFARI_HOME/solr
export SOLR_ENV=$SOLR_INSTALL_DIR/bin/solr.in.sh
export SOLR_INCLUDE=$SOLR_ENV
export CASSANDRA_HOME=$DATAFARI_HOME/cassandra
export CASSANDRA_ENV=$CASSANDRA_HOME/bin/cassandra.in.sh
export CASSANDRA_INCLUDE=$CASSANDRA_ENV
export CASSANDRA_HOST=127.0.0.1
export CASSANDRA_PORT=9042
export POSTGRES_HOME=$DATAFARI_HOME/pgsql
export ZK_HOME=$DATAFARI_HOME/zookeeper
export ZK_ENV=$ZK_HOME/bin/zkEnvh.sh
export ZK_HOME_MCF=$DATAFARI_HOME/zookeeper-mcf
export ZK_ENV_MCF=$ZK_HOME_MCF/bin/zkEnvh.sh
export ELK_HOME=$DATAFARI_HOME/elk
export TIKA_SERVER_HOME=$DATAFARI_HOME/tika-server

# Logs directory: needed for Cassandra startup
export DATAFARI_LOGS=$DATAFARI_HOME/logs

# Configs
export CONFIG_HOME=$TOMCAT_HOME/conf
export TOMCAT_MCF_CONFIG_HOME=$TOMCAT_MCF_HOME/conf
export MAIN_DATAFARI_CONFIG_HOME=$TOMCAT_HOME/conf
export DATAFARI_SOLR_PROPERTIES_HOME=$TOMCAT_HOME/conf


# pid files 
export MCF_PID_FILE=$PID_DIR/mcf_crawler_agent.pid
export CATALINA_PID=$PID_DIR/tomcat.pid
export CATALINA_MCF_PID=$PID_DIR/tomcat-mcf.pid
export SOLR_PID_FILE=$PID_DIR/solr-8983.pid
export CASSANDRA_PID_FILE=$PID_DIR/cassandra.pid
export POSTGRES_PID_FILE=$PID_DIR/postmaster.pid
export ZK_PID_FILE=$PID_DIR/zookeeper-solr.pid
export ZK_MCF_PID_FILE=$PID_DIR/zookeeper-mcf.pid
if [ -d /etc/apache2 ]; then
	export APACHE_PID_FILE=$PID_DIR/apache2.pid
elif [ -d /etc/httpd ]; then
	export APACHE_PID_FILE=$PID_DIR/apache/apache2.pid
fi

export RETRIES_NUMBER=35