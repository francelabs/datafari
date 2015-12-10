#!/bin/bash -e
#
#
# Startup script for Datafari
#
#

if (( EUID != 0 )); then
   echo "You need to be root to run this script." 1>&2
   exit 100
fi

DIR=../../../debian7/bin

source "set-datafari-env-devmode.sh"
source "${DIR}/utils.sh"


if is_running $SOLR_PID_FILE; then
   echo "Error : Solr seems to be running already with PID $(cat $SOLR_PID_FILE)"
   exit 1
fi

if is_running $CASSANDRA_PID_FILE; then
   echo "Error : Cassandra seems to be running already with PID $(cat $CASSANDRA_PID_FILE)"
   exit 1
fi


CASSANDRA_INCLUDE=$CASSANDRA_ENV $CASSANDRA_HOME/bin/cassandra -p $CASSANDRA_PID_FILE
SOLR_INCLUDE=$SOLR_ENV $SOLR_INSTALL_DIR/bin/solr start -a "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=4044"
