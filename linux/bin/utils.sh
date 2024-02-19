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

check_python()
{
if command -v python3 &> /dev/null
then
  echo "Compatible Python version detected : Python3"
  python_version=3
else
  version=$(python -V 2>&1 | grep -Po '(?<=Python )(.+)')
  if [[ -z "$version" ]]
  then
    echo "No Python detected! Please install Python 2.7.x"
    exit 1
  else
    case "$(python --version 2>&1)" in
    *" 2.7"*)
      echo "Compatible Python version detected : Python > 2.7"
      python_version=2
      ;;
    *)
      echo "Wrong Python version! Please install Python 2.7.X"
      exit 1
      ;;
    esac
  fi
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

awk -v pat="^$1=" -v value="$1=$2" '{ if ($0 ~ pat) print value; else print $0; }' "$file" > "$file".tmp
mv "$file".tmp "$file"

if  [[ "$3" = *datafari.properties* ]]; then
  chown ${DATAFARI_USER} $3
    chmod 775 $3
fi
                
}

