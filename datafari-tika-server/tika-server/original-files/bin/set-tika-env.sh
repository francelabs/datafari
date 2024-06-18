#!/bin/bash
#
# Tika env
#

export TIKA_MEM="-Xmx512m"
export TIKA_SERVER_HOME=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
export PID_DIR=$DATAFARI_HOME/pid
export TIKA_SERVER_PID_FILE=$PID_DIR/tika.pid
export TIKA_LOGS_DIR=$TIKA_SERVER_HOME/logs
export PATH=${PATH}:$JAVA_HOME/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
