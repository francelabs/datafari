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

waitCassandra() {
	echo "Checking if Cassandra is up and running ..."
	# Try to connect on Cassandra's JMX port 7199 and CQLSH port 9042
	cassandra_status=0
	retries=1

	exec 6<>/dev/tcp/localhost/7199 || cassandra_status=1
	exec 6>&- # close output connection
	exec 6<&- # close input connection

	exec 6<>/dev/tcp/localhost/9042 || cassandra_status=1
	exec 6>&- # close output connection
	exec 6<&- # close input connection

	while (( retries < 10 && cassandra_status != 0 )); do
		echo "Cassandra doesn't reply to requests on ports 7199 and/or 9042. Sleeping for a while and trying again... retry ${retries}"

		cassandra_status=0

		# Sleep for a while
		sleep 5s

		exec 6<>/dev/tcp/localhost/7199 || cassandra_status=1
		exec 6>&- # close output connection
		exec 6<&- # close input connection
		
		exec 6<>/dev/tcp/localhost/9042 || cassandra_status=1
		exec 6>&- # close output connection
		exec 6<&- # close input connection

		((retries++))
	done

	if [ $cassandra_status -ne 0 ]; then
		echo "/!\ ERROR: Cassandra startup has ended with errors; please check log file ${DATAFARI_LOGS}/cassandra-startup.log"
	else
		echo "Cassandra startup completed successfully --- OK"
	fi
}

waitElasticsearch() {
	echo "Checking if Elasticsearch is up and running ..."
	# Try to connect on Elasticsearch port 9200
	elasticsearch_status=0
	retries=1

	exec 6<>/dev/tcp/localhost/9200 || elasticsearch_status=1
	exec 6>&- # close output connection
	exec 6<&- # close input connection

	while (( retries < 6 && elasticsearch_status != 0 )); do
		echo "Elasticsearch doesn't reply to requests on port 9200. Sleeping for a while and trying again... retry ${retries}"

		elasticsearch_status=0

		# Sleep for a while
		sleep 10s

		exec 6<>/dev/tcp/localhost/9200 || elasticsearch_status=1
		exec 6>&- # close output connection
		exec 6<&- # close input connection

		((retries++))
	done

	if [ $elasticsearch_status -ne 0 ]; then
		echo "/!\ ERROR: Elasticsearch startup has ended with errors"
		exit 1;
	else
		echo "Elasticsearch startup completed successfully --- OK"
	fi
}

waitKibana() {
	echo "Checking if Kibana is up and running ..."
	# Try to connect on Kibana port 5601
	kibana_status=0
	retries=1

	exec 6<>/dev/tcp/@NODEHOST@/5601 || kibana_status=1
	exec 6>&- # close output connection
	exec 6<&- # close input connection

	while (( retries < 6 && kibana_status != 0 )); do
		echo "Kibana doesn't reply to requests on port 5601. Sleeping for a while and trying again... retry ${retries}"

		kibana_status=0

		# Sleep for a while
		sleep 10s

		exec 6<>/dev/tcp/@NODEHOST@/5601 || kibana_status=1
		exec 6>&- # close output connection
		exec 6<&- # close input connection

		((retries++))
	done

	if [ $kibana_status -ne 0 ]; then
		echo "/!\ ERROR: Kibana startup has ended with errors"
		exit 1;
	else
		echo "Kibana startup completed successfully --- OK"
	fi
}

is_running() {
    local pidFile=$1
    if ! [ -f $pidFile ]; then
	return 1
    fi
    local pid
    pid=$(sudo su datafari -c "cat $pidFile")
    if ! ps -p $pid 1>/dev/null 2>&1; then
        echo "Warn: a PID file was detected, removing it."
        sudo su datafari -c "rm -f $pidFile"
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
    pid=$(sudo su datafari -c "cat $pidFile")
    sudo su datafari -c "kill $pid"
    waitpid $pid 30 .
    if [ $? -ne 0 ]; then
        echo
        echo "Warn: failed to stop $2 in 30 seconds, sending SIGKILL"
        sudo su datafari -c "kill -9 $pid"
        sleep 1
    fi
    echo "stopped"
    sudo su datafari -c "rm -f $pidFile"
}
