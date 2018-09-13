#!/bin/bash

# util funcs
check_java()
{
  if type -p java; then
    echo found java executable in PATH
    _java=java
  elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
      echo found java executable in JAVA_HOME
      _java="$JAVA_HOME/bin/java"
  else
      echo "no Java detected. Please install Java. Program will exit."
      exit
  fi
  
  if [[ "$_java" ]]; then
      version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
      echo version "$version"
      if [[ "$version" > "1.8" ]]; then
          echo Java version detected is OK
  
      else
          echo Java version is not >=1.8. Please install at least Java 8. Program will exit
          exit
      fi
  fi
  
  if [ -e "$JAVA_HOME"/bin/java ]; then
    echo "JAVA HOME is correctly set"
  else
    echo "Environment variable JAVA_HOME is not properly set." 1>&2
    exit 1
  fi
}

check_python()
{
  version=$(python -V 2>&1 | grep -Po '(?<=Python )(.+)')
  if [[ -z "$version" ]]
  then
    echo "No Python detected! Please install Python 2.7.x"
    exit 1
  else
    case "$(python --version 2>&1)" in
    *" 2.7"*)
      echo "Compatible Python version detected"
      ;;
    *)
      echo "Wrong Python version! Please install Python 2.7.X"
      exit 1
      ;;
    esac
  fi
}

run_as()
{
  user=$1
  command=$2
  current_user=`whoami`
  if [ $current_user != $user ] ; then
    sudo -E su $user -p -c "$command"
  else
    $command
  fi
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

waitTomcat() {
  echo "Checking if Tomcat is up and running ..."
  # Try to connect to Tomcat on port 8080
  tomcat_status=0
  retries=1

  exec 6<>/dev/tcp/localhost/8080 || tomcat_status=1
  exec 6>&- # close output connection
  exec 6<&- # close input connection

  while (( retries < 10 && tomcat_status != 0 )); do
    echo "Tomcat doesn't reply to requests on port 8080. Sleeping for a while and trying again... retry ${retries}"

    tomcat_status=0

    # Sleep for a while
    sleep 5s

    exec 6<>/dev/tcp/localhost/8080 || tomcat_status=1
    exec 6>&- # close output connection
    exec 6<&- # close input connection

    ((retries++))
  done

  if [ $tomcat_status -ne 0 ]; then
    echo "/!\ ERROR: Tomcat startup has ended with errors; please check log file ${DATAFARI_LOGS}/tomcat.log"
  else
    echo "Tomcat startup completed successfully --- OK"
  fi
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
