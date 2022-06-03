#!/bin/bash -e
#
#
# Set Datafari environment variable
#
#

export DATAFARI_HOME=$( cd "$( dirname "${BASH_SOURCE[0]}" )/../.." && pwd )

#Global vars
export PID_DIR=$DATAFARI_HOME/pid
export AS_HOME=$DATAFARI_HOME/analytic-stack
export AS_LOGS=$DATAFARI_HOME/logs
export ZEPPELIN_LOG_DIR=$AS_LOGS
export LOGSTASH_HOME=$AS_HOME/logstash
export ZEPPELIN_HOME=$AS_HOME/zeppelin
export TIKA_SERVER_HOME=$DATAFARI_HOME/tika-server
export LS_JAVA_HOME=$JAVA_HOME

#Zeppelin specs
export ZEPPELIN_JAVA_OPTS="-Duser.timezone=UTC"
export ZEPPELIN_PORT=8888
export ZEPPELIN_MEM="-Xms1024m -Xmx1024m -XX:MaxMetaspaceSize=512m"

# pid files 
export LOGSTASH_PID_FILE=$PID_DIR/logstash.pid
export ZEPPELIN_PID_DIR=$PID_DIR

export RETRIES_NUMBER=35