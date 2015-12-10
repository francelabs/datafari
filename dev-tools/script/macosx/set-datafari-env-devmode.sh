#!/bin/bash -e
#
#
# Set Datafari environment variable
#
#

export DATAFARI_HOME=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/../../..
export JAVA_HOME=${DATAFARI_HOME}/macosx/jvm
export LD_LIBRARY_PATH=${DATAFARI_HOME}/pgsql/lib:${DATAFARI_HOME}/ocr/tesseract/lib:${DATAFARI_HOME}/ocr/leptonica/lib
export INIT_STATE_FILE=${DATAFARI_HOME}/bin/common/init_state.properties
export CONFIG_FILE=${DATAFARI_HOME}/tomcat/conf/datafari.properties
export TESSDATA_PREFIX=${DATAFARI_HOME}/ocr/tesseract/share
export LC_NUMERIC=C
export PID_DIR=$DATAFARI_HOME/pid
export TOMCAT_HOME=$DATAFARI_HOME/tomcat
export MCF_HOME=$DATAFARI_HOME/mcf/mcf_home
export SOLR_INSTALL_DIR=../../../solr
export SOLR_ENV=$SOLR_INSTALL_DIR/bin/solr.in.sh
export CASSANDRA_HOME=$DATAFARI_HOME/cassandra
export CASSANDRA_ENV=$CASSANDRA_HOME/bin/cassandra.in.sh

# pid files 
export MCF_PID_FILE=$PID_DIR/mcf_crawler_agent.pid
export CATALINA_PID=$PID_DIR/tomcat.pid
export SOLR_PID_FILE=$PID_DIR/solr-8983.pid
export CASSANDRA_PID_FILE=$PID_DIR/cassandra.pid
export POSTGRES_PID_FILE=$PID_DIR/postmaster.pid

