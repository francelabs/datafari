#!/bin/bash

# util funcs
waitpid() {
    local pid=$1 timeout=$2 
    { [ -z "$pid" ] || [ -z "$timeout" ]; } && return 10
    local t=0
    while ps -p $pid 1>/dev/null 2>&1; do
        sleep 1
        t=$((t + 1))
        if [ $t -eq $timeout ]; then
            return 1
        fi
        echo -n "."
    done
    return 0
}

waitTomcat() {
    until [ "`curl --silent --show-error --connect-timeout 1 -I http://localhost:8080 | grep 'Coyote'`" != "" ];
    local t=9 timeout=15
    do
        t=$((t + 1))
        if [ $t -eq $timeout ]; then
            return 1
        fi
        sleep 3
    done
}

is_running() {
    local pidFile=$1
    if ! [ -f $pidFile ]; then
	return 1
    fi
    local pid
    pid=$(cat $pidFile)
    if ! ps -p $pid 1>/dev/null 2>&1; then
        echo "Warn: a PID file was detected, removing it."
        rm -f $pidFile
        return 1
    fi
    return 0        
}

forceStopIfNecessary(){
    local pidFile=$1
    if ! [ -f $pidFile ]; then
        return 0
    fi
    local pid
    pid=$(cat $pidFile)
    kill $pid
    waitpid $pid 30 .
    if [ $? -ne 0 ]; then
        echo
        echo "Warn: failed to stop $2 in 30 seconds, sending SIGKILL"
        kill -9 $pid
        sleep 1
    fi
    echo "stopped"
    rm -f $pidFile
}
