#!/bin/bash -e
#
#
# Set Datafari environment variable
#
#

export DATAFARI_HOME=$( cd "$( dirname "${BASH_SOURCE[0]}" )/../.." && pwd )

export PID_DIR=$DATAFARI_HOME/pid
export ELK_HOME=$DATAFARI_HOME/elk
export ELK_LOGS=$ELK_HOME/logs
export ELASTICSEARCH_HOME=$ELK_HOME/elasticsearch
export LOGSTASH_HOME=$ELK_HOME/logstash
export KIBANA_HOME=$ELK_HOME/kibana

# pid files 
export ELASTICSEARCH_PID_FILE=$PID_DIR/elasticsearch.pid
export LOGSTASH_PID_FILE=$PID_DIR/logstash.pid
export KIBANA_PID_FILE=$PID_DIR/kibana.pid

# Kibana max mem
export NODE_OPTIONS="--max-old-space-size=512"

export RETRIES_NUMBER=35