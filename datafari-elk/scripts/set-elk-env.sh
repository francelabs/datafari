#!/bin/bash -e
#
#
# Set Datafari environment variable
#
#

export DATAFARI_HOME=$( cd "$( dirname "${BASH_SOURCE[0]}" )/../.." && pwd )

if [ -d "${DATAFARI_HOME}/jvm" ]; then
  export JAVA_HOME=${DATAFARI_HOME}/jvm
fi

export PID_DIR=$DATAFARI_HOME/pid
export ELASTICSEARCH_HOME=$DATAFARI_HOME/elk/elasticsearch
export LOGSTASH_HOME=$DATAFARI_HOME/elk/logstash
export KIBANA_HOME=$DATAFARI_HOME/elk/kibana

# pid files 
export ELASTICSEARCH_PID_FILE=$PID_DIR/elasticsearch.pid
export LOGSTASH_PID_FILE=$PID_DIR/logstash.pid
export KIBANA_PID_FILE=$PID_DIR/kibana.pid

