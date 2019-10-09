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

spin()
{
  spinner="/|\\-/|\\-"
  while :
  do
    for i in `seq 0 180`
    do
      echo -n "${spinner:$i:1}"
      echo -en "\010"
      sleep 1
    done
  done
}

waitElasticsearch() {
  echo "Waiting up until 180 seconds to see Elasticsearch running..."
  spin &
  SPIN_ELASTICSEARCH_PID=$!
  # Try to connect to Elasticsearch on port 9200
  elasticsearch_status=1
  retries=1

  while (( retries < ${RETRIES_NUMBER} && elasticsearch_status != 0 )); do
  
  elasticsearch_status=0
  # Sleep for a while
  sleep 5s
  { exec 6<>/dev/tcp/localhost/9200; } > /dev/null 2>&1 || elasticsearch_status=1
    exec 6>&- # close output connection
    exec 6<&- # close input connection
    ((retries++))
  done
  
  kill $SPIN_ELASTICSEARCH_PID
  wait $SPIN_ELASTICSEARCH_PID 2>/dev/null

  if [ $elasticsearch_status -ne 0 ]; then
    echo "/!\ ERROR: Elasticsearch startup has ended with errors; please check log file ${ELK_LOGS}/elasticsearch.log"
  else
    echo "Elasticsearch startup completed successfully --- OK"
    sleep 2
  fi
}


waitKibana() {
  echo "Waiting up until 180 seconds to see Kibana running..."
  spin &
  SPIN_KIBANA_PID=$!
  # Try to connect to Kibana on port 5601
  kibana_status=1
  retries=1

  while (( retries < ${RETRIES_NUMBER} && kibana_status != 0 )); do
  
  kibana_status=0
  # Sleep for a while
  sleep 5s
  { exec 6<>/dev/tcp/localhost/5601; } > /dev/null 2>&1 || kibana_status=1
    exec 6>&- # close output connection
    exec 6<&- # close input connection
    ((retries++))
  done
  
  kill $SPIN_KIBANA_PID
  wait $SPIN_KIBANA_PID 2>/dev/null

  if [ $kibana_status -ne 0 ]; then
    echo "/!\ ERROR: Kibana startup has ended with errors; please check log file ${ELK_LOGS}/kibana.log"
  else
    echo "Kibana startup completed successfully --- OK"
    sleep 2
  fi
}

