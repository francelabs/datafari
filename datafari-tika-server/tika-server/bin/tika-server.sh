#!/bin/bash
#
# Tika Server Manager 
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/set-tika-env.sh"
source "${DIR}/utils.sh"


cmd_start() {
  if is_running $TIKA_SERVER_PID_FILE; then
   PID=$(cat $TIKA_SERVER_PID_FILE);
   echo "Error : Tika Server seems to be already running with PID $PID"
   exit 1
  fi
  echo "Starting Tika Server"
  nohup "$JAVA_HOME/bin/java" -Dlog4j2.configurationFile=file:$TIKA_SERVER_HOME/conf/log4j2.properties.xml -Duser.timezone=UTC $TIKA_MEM -cp $TIKA_SERVER_HOME/bin/tika-server.jar org.apache.tika.server.core.TikaServerCli -c $TIKA_SERVER_HOME/conf/tika-config.xml >/dev/null 2>&1 &
  echo $! > $TIKA_SERVER_PID_FILE
  echo "Tika Server started with PID $(cat $TIKA_SERVER_PID_FILE)"
  return 0
}

cmd_stop() {
  if is_running $TIKA_SERVER_PID_FILE; then
    echo -n "Stopping Tika Server ..."
    forceStopIfNecessary $TIKA_SERVER_PID_FILE Tika-Server
  else
    echo "Warn : Tika server does not seem to be running."
  fi
}

cmd_status() {
    if is_running $TIKA_SERVER_PID_FILE; then
        echo "Tika Server is running:"
        ps -o pid,cmd --width 5000 -p $(cat $BATCH_PID_FILE)
    else
        echo "Tika Server is not running."
    fi
}


COMMAND=$1

case $COMMAND in
    start)
        cmd_start
        ;;
    stop)
        cmd_stop
        ;;
    status)
        cmd_status
        ;;
esac
