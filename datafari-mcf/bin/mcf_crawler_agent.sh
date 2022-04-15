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
    nohup "$JAVA_HOME/bin/java" $OPTIONS org.apache.manifoldcf.agents.AgentRun > $DATAFARI_HOME/logs/mcf-agent.log 2>&1 &
    echo $! > $MCF_PID_FILE
	echo "MCF Agent started with PID $(cat $MCF_PID_FILE)"
    return 0
}

cmd_stop() {
    echo -n "Stopping MCF Agent ..."
    ./executecommand.sh org.apache.manifoldcf.agents.AgentStop
    #Wait few seconds to let the MCF agent stopping by itself
    sleep 10
    mcf_pid=$(cat $MCF_PID_FILE)
    if ps -p $mcf_pid > /dev/null
    then
       echo "Could not stop the MCF crawler agent process ! It's PID file will not be removed !"
    else
      rm -f $MCF_PID_FILE
    fi
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

