#!/bin/bash
#
#
# MCF control script
#
#

cd $MCF_HOME

OPTIONSFILE="options.env.unix"
OPTIONS=$(cat "$OPTIONSFILE")



cmd_start() {
    echo "Starting MCF Agent ..."
    ./executecommand.sh org.apache.manifoldcf.core.LockClean
    start-stop-daemon --background --chdir=$MCF_HOME --start --make-pidfile --pidfile $MCF_PID_FILE --exec "$JAVA_HOME/bin/java" -- $OPTIONS org.apache.manifoldcf.agents.AgentRun
    sleep 1
	echo "MCF Agent started with PID $(cat $MCF_PID_FILE)"
    return 0
}

cmd_stop() {
    echo -n "Stopping MCF Agent ..."
    ./executecommand.sh org.apache.manifoldcf.agents.AgentStop
    rm -f $MCF_PID_FILE
}

cmd_status() {
    if is_running; then
        echo "MCF Agent is running:"
        ps -o pid,cmd --width 5000 -p $(cat $MCF_PID_FILE)
    else
        echo "MCF Agent is not running."
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
esac

