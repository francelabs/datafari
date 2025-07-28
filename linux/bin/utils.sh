#!/bin/bash

# util funcs
service_exists() {
    local n=$1
    if [[ $(systemctl list-units --all -t service --full --no-legend "$n.service" | sed 's/^\s*//g' | cut -f1 -d' ') == $n.service ]]; then
        return 0
    else
        return 1
    fi
}

check_service_tcp() {
  local name="$1"
  local host="$2"
  local port="$3"
  local result

  if (echo > /dev/tcp/"$host"/"$port") >/dev/null 2>&1; then
    result="✅ OK         "
  else
    result="❌ KO         "
    STATUS=1
  fi

  echo "│ $(printf '%-20s' "$name") │ $result│"
}


check_service() {
  local name="$1"
  local test_cmd="$2"
  local result

  if eval "$test_cmd"; then
    result="✅ OK         "
  else
    result="❌ KO         "
    STATUS=1
  fi

  echo "│ $(printf '%-20s' "$name") │ $result│" 
}

check_service_pid() {
  local name="$1"
  local pidfile="$2"
  local result

if [[ -f "$pidfile" ]]; then
    pid=$(cat "$pidfile" 2>/dev/null)
    if [[ "$pid" =~ ^[0-9]+$ ]] && ps -p "$pid" > /dev/null 2>&1; then
result="✅ OK         "
  else
    result="❌ KO         "
    STATUS=1
  fi
else
 result="❌ KO         "
    STATUS=1
  fi

echo "│ $(printf '%-20s' "$name") │ $result│" 
}

check_services() {

echo "┌──────────────────────┬───────────────┐"
echo "│ Component            │ State         │" 
echo "├──────────────────────┼───────────────┤" 

STATUS=0
if  [[ "$NODETYPE" != *solr* ]]; then
  check_service "Tomcat"    "curl -s http://localhost:8080 > /dev/null"
  check_service "Tomcat-MCF"    "curl -s http://localhost:9080 > /dev/null"
  if  [[ "$NODETYPE" == "monoserver" ]] ; then
    check_service "Solr" "curl -s -o /dev/null -w '%{http_code}' http://localhost:8983/solr/FileShare/select?q=datafarirocks\&rows=0 | grep -q 200"
  fi
  check_service_tcp "Zookeeper"     localhost 2181
  check_service_tcp "Zookeeper-MCF" localhost 2182
  check_service_tcp "Cassandra"     localhost 9042
  check_service_tcp "PostgreSQL"    localhost 5432
  check_service_tcp "Tika Server"   localhost 9998
  check_service_pid "MCF Agent"  $DATAFARI_HOME/pid/mcf_crawler_agent.pid
  if  [[ "$AnalyticsActivation" = *true* ]]; then
    check_service_pid "Logstash"  $DATAFARI_HOME/pid/logstash.pid
  fi
  check_service "Apache"      "curl -s --insecure https://localhost/datafariui > /dev/null"

elif [[ "$NODETYPE" == *solr* ]]; then
  check_service "Solr" "curl -s -o /dev/null -w '%{http_code}' http://localhost:8983/solr/FileShare/select?q=datafarirocks\&rows=0 | grep -q 200"
  check_service_tcp "Zookeeper"     localhost 2181
  check_service "Apache"      "curl -s --insecure https://localhost/datafariui > /dev/null"
  if  [[ "$AnalyticsActivation" = *true* ]]; then
    check_service_pid "Logstash"  $DATAFARI_HOME/pid/logstash.pid
  fi
fi

echo "└──────────────────────┴───────────────┘" 

if [ "$STATUS" -eq 0 ]; then
  echo "[CHECK] ✅ All services are ready. Start OK." 
else
  echo "[CHECK] ❌ Some services seem  to be missing or unfunctional, check them"
fi

}

# helper function
is_locale_available() {
  locale -a | grep -qi "^$1$"
}

# Check locales
ensure_valid_locales() {
  default_locale="C.UTF-8"

  if [ -z "$LANG" ] || ! is_locale_available "$LANG"; then
    export LANG="$default_locale"
    echo "INFO: LANG is unset or invalid, setting to $LANG"
  fi

  if [ -z "$LC_ALL" ] || ! is_locale_available "$LC_ALL"; then
    export LC_ALL="$LANG"
    echo "INFO: LC_ALL is unset or invalid, setting to $LC_ALL"
  fi
}
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
      version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
      echo "Java version detected : $version"
      if [[ "$version" -eq "11" ]]; then
          echo "Java version detected $version : OK"

      else
          echo "Java version installed is not Java 11. Please install ONLY Java 11.See this page for more information : https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/110788634/Software+requirements. Program will exit"
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

check_ram()
{
  sizeMemory=$(grep MemTotal /proc/meminfo | awk '{print $2}')
  if [[ "$sizeMemory" -lt "8120344" ]]; then
      echo The memory detected on your system seems very low. Please be sure that the requirements are respected. See this page : https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/1662451718/Hardware+requirements
        echo "Wait 10 seconds"
        sleep 10
    else
      echo "RAM size detected $sizeMemory KB : OK"
    fi 
}

check_python() {
  python_cmd=""
  python_version=""
 
  if command -v python3 &> /dev/null; then
    python_cmd="python3"
  elif command -v python &> /dev/null; then
    python_cmd="python"
  else
    echo "No Python interpreter found. Please install Python 3."
    exit 1
  fi

  # Récupération de la version complète
  version=$($python_cmd -V 2>&1 | grep -Po '(?<=Python )(.+)')
  major=$(echo "$version" | cut -d. -f1)
  minor=$(echo "$version" | cut -d. -f2)

  if [[ "$major" -eq 3 ]]; then
    if [[ "$minor" -eq 12 ]]; then
      echo "Special notice: Python version 3.12.x detected! Version incompatible with Cassandra so Datafari will not be working correctly. See https://gitlab.datafari.com/datafari-community/datafari/-/issues/1007"
      echo "The script will exit"
      exit 1
    fi
    echo "Compatible Python version detected: Python $version"
    python_version=3
  else
    echo "Unsupported Python version detected: Python $version. Only Python 3 is supported."
    exit 1
  fi

  set_property "python_version" $python_version $CONFIG_FILE
}
run_as()
{
  user=$1
  command=$2
  current_user=`whoami`
  if [ $current_user != $user ] ; then
    sudo -E su $user -c "$command"
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

waitTomcat() {
  echo "Waiting up until 180 seconds to see Tomcat running..."
  spin &
  SPIN_TOMCAT_PID=$!
  # Try to connect to Tomcat on port 8080
  tomcat_status=1
  retries=1

  while (( retries < ${RETRIES_NUMBER} && tomcat_status != 0 )); do
  
  tomcat_status=0
  # Sleep for a while
  sleep 5s
  { exec 6<>/dev/tcp/localhost/8080; } > /dev/null 2>&1 || tomcat_status=1
    exec 6>&- # close output connection
    exec 6<&- # close input connection
    retries=$((retries+1))
  done
  
  kill -s PIPE "$SPIN_TOMCAT_PID" &

  if [ $tomcat_status -ne 0 ]; then
    echo "/!\ ERROR: Tomcat startup has ended with errors; please check log file ${DATAFARI_LOGS}/tomcat.log"
  else
    echo "Tomcat startup completed successfully --- OK"
    sleep 2
  fi
}

waitTomcatMCF() {
  echo "Waiting up until 180 seconds to see Tomcat-MCF running..."
  spin &
  SPIN_TOMCAT_MCF_PID=$!
  # Try to connect to Tomcat on port 9080
  tomcat_mcf_status=1
  retries=1

  while (( retries < ${RETRIES_NUMBER} && tomcat_mcf_status != 0 )); do
  
  tomcat_mcf_status=0
  # Sleep for a while
  sleep 5s
  { exec 6<>/dev/tcp/localhost/9080; } > /dev/null 2>&1 || tomcat_mcf_status=1
    exec 6>&- # close output connection
    exec 6<&- # close input connection
    retries=$((retries+1))
  done
  
  kill -s PIPE "$SPIN_TOMCAT_MCF_PID"

  if [ $tomcat_mcf_status -ne 0 ]; then
    echo "/!\ ERROR: Tomcat-MCF startup has ended with errors; please check log file ${DATAFARI_LOGS}/tomcat.log"
  else
    echo "Tomcat-MCF startup completed successfully --- OK"
    sleep 2
  fi
}

waitCassandra() {
  echo "Waiting up until 180 seconds to see Cassandra running..."
  spin &
  SPIN_CASSANDRA_PID=$!
  # Try to connect on Cassandra's JMX port 7199 and CQLSH port 9042
  cassandra_status=1
  retries=1

  while (( retries < ${RETRIES_NUMBER} && cassandra_status != 0 )); do
  cassandra_status=0
  # Sleep for a while
  sleep 5s
  { exec 6<>/dev/tcp/localhost/9042; } > /dev/null 2>&1 || cassandra_status=1
  exec 6>&- # close output connection
  exec 6<&- # close input connection
  { exec 6<>/dev/tcp/localhost/7199; } > /dev/null 2>&1 || cassandra_status=1
  exec 6>&- # close output connection
  exec 6<&- # close input connection
    retries=$((retries+1))
  done
  kill -s PIPE "$SPIN_CASSANDRA_PID" &
  
  if [ $cassandra_status -ne 0 ]; then
    echo "/!\ ERROR: Cassandra startup has ended with errors; please check log file ${DATAFARI_LOGS}/cassandra-startup.log"
  else
    echo "Cassandra startup completed successfully --- OK"
    sleep 2
  fi
}




setProperty(){
  awk -v pat="^$1=" -v value="$1=$2" '{ if ($0 ~ pat) print value; else print $0; }' $3 > $3.tmp
  mv $3.tmp $3
}

is_variable_set() {
    if [ -z "$1" ]; then 
        echo "A required property is not correctly filled in datafari.properties file. The script will exit."; 
        exit 1;
    fi
}

is_file_present() {
    if  ! test -f "$1"; then
        echo "The file $1 is not present or cannot be read. The script will exit.";
        exit 1;
    fi
}

getProperty() {
    awk -F'=' -v k="$1" '$1==k&&sub(/^[^=]*=/,"")' $2
}


set_property() {
if [ -z "$1" ]; then
  echo "No parameters provided, exiting..."
  exit 1
fi
if [ -z "$2" ]; then
  echo "Key provided, but no value, breaking"
  exit 1
fi
if [ -z "$3" ] && [ -z "$setPropertyFile" ]; then
  echo "No file provided or setPropertyFile is not set, exiting..."
  exit 1
fi

if [ "$setPropertyFile" ] && [ "$3" ]; then
    echo "setPropertyFile variable is set AND filename in comamnd! Use only or the other. Exiting..."
    exit 1
else
  if [ "$3" ] && [ ! -f "$3" ]; then
    echo "File in command NOT FOUND!"
    exit 1
  elif [ "$setPropertyFile" ] && [ ! -f "$setPropertyFile" ]; then
    echo "File in setPropertyFile variable NOT FOUND!"
    exit 1
  fi
fi

if [ "$setPropertyFile" ]; then
  file=$setPropertyFile
else
  file=$3
fi

modifValue=$2
modifValue="${modifValue//#/\\#}"
modifValue="${modifValue//=/\\=}"
modifValue="${modifValue//:/\\:}"

awk -v pat="^$1=" -v value="$1=$modifValue" '{ if ($0 ~ pat) print value; else print $0; }' "$file" > "$file".tmp
mv "$file".tmp "$file"

if  [[ "$3" = *datafari.properties* ]]; then
  chown ${DATAFARI_USER} $3
    chmod 775 $3
fi
                
}

