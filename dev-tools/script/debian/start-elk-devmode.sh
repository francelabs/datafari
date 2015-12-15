#!/bin/bash -e
#
#
# Startup script for Datafari ELK layer
#
#

DIR=../../../debian7/bin

source "set-datafari-env-devmode.sh"
source "${DIR}/utils.sh"

if is_running $ELASTICSEARCH_PID_FILE; then
   echo "Error : Elasticsearch seems to be running already with PID $(cat $ELASTICSEARCH_PID_FILE)"
   exit 1
fi

if is_running $LOGSTASH_PID_FILE; then
   echo "Error : Logstash seems to be already running with PID $(cat $LOGSTASH_PID_FILE)"
   exit 1
fi

if is_running $KIBANA_PID_FILE; then
   echo "Error : Kibana seems to be already running with PID $(cat $KIBANA_PID_FILE)"
   exit 1
fi

cd $ELASTICSEARCH_HOME/bin
bash elasticsearch -p $ELASTICSEARCH_PID_FILE &

cd $LOGSTASH_HOME
bash bin/logstash agent -f $LOGSTASH_HOME/logstash.conf &
LOGSTASH_PID=`ps ux | grep logstash | grep java | grep agent | awk '{ print $2}'`
if [ "x$LOGSTASH_PID" = "x" ]; then
LOGSTASH_PID=-1
fi
if [ $LOGSTASH_PID -ne -1 ]; then
	echo $LOGSTASH_PID > $LOGSTASH_PID_FILE
fi

cd $KIBANA_HOME/bin
bash kibana &
