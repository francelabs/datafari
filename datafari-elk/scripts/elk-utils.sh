#!/bin/bash

# util funcs
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
