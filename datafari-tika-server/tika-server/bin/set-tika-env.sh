#!/bin/bash
#
# Tika env
#

export DATAFARI_HOME=$( cd "$( dirname "${BASH_SOURCE[0]}" )/../.." && pwd )
export TMP_DIR=/tmp
export TIKA_MEM="-Xmx512m"
export TIKA_PORT=9998
export DO_OCR=false
export TIKA_SERVER_HOME=$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )
export OCR_OPTION="-c $TIKA_SERVER_HOME/conf/tika.conf"
export PID_DIR=$DATAFARI_HOME/pid
export TIKA_SERVER_PID_FILE=$PID_DIR/tika.pid
export TIKA_LOGS_DIR=$DATAFARI_HOME/logs/tika-server
export TIKA_SPAWN="-spawnChild"
export TIKA_SPAWN_MEM="-JXms5120m"
# Activate only on Tika 1.19.1
export TIKA_SPAWN_FILES="-maxFiles"
export TIKA_SPAWN_TASK_TIMEOUT="-taskTimeoutMillis 120000"
export TIKA_SPAWN_PING_TIMEOUT="-pingTimeoutMillis 120000"