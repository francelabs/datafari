#!/bin/bash -e
#
#
# Shutdown script for Datafari
#
#

DIR=../../../debian7/bin

source "set-datafari-env-devmode.sh"
source "${DIR}/utils.sh"


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
