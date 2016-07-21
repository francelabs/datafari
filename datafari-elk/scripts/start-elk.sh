DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/set-elk-env.sh"
source "${DIR}/elk-utils.sh"

if is_running $ELASTICSEARCH_PID_FILE; then
   echo "Error : Elasticsearch seems to be running already with PID $(cat $ELASTICSEARCH_PID_FILE)"
   exit 1
fi

if is_running $LOGSTASH_PID_FILE; then
   echo "Error : Logstash seems to be already running with PID $(cat $LOGSTASH_PID_FILE)"
   exit 1
fi

if is_running $KIBANA_PID_FILE; then
   echo "Error : Kibana seems to be already running with PID $(cat $KIBANA_PID_FILE)"
   exit 1
fi

cd $ELASTICSEARCH_HOME/bin
bash elasticsearch -p $ELASTICSEARCH_PID_FILE &
sleep 5

# Replace the default conf with the correct paths before starting logstash
sed -i "/francelabs\/datafari-stats.log/c\	path => \"${DATAFARI_HOME}/logs/datafari-stats.log\"" $LOGSTASH_HOME/logstash-datafari.conf
sed -i "/francelabs\/datafari-monitoring.log/c\	path => \"${DATAFARI_HOME}/logs/datafari-monitoring.log\"" $LOGSTASH_HOME/logstash-datafari.conf
cd $LOGSTASH_HOME
bash bin/logstash agent -f $LOGSTASH_HOME/logstash-datafari.conf &
# Must sleep 1 sec to be sure to find logstash's PID
sleep 1
LOGSTASH_PID=`ps ux | grep logstash | grep java | grep agent | awk '{ print $2}'`
if [ "x$LOGSTASH_PID" = "x" ]; then
LOGSTASH_PID=-1
fi
if [ $LOGSTASH_PID -ne -1 ]; then
	echo $LOGSTASH_PID > $LOGSTASH_PID_FILE
fi

# Configure the right path for Kibana PID file
sed -i "/pid\.file/c\pid.file: ${KIBANA_PID_FILE}" $KIBANA_HOME/config/kibana.yml
sed -i "/server\.crt/c\server.ssl.cert: ${DATAFARI_HOME}/ssl-keystore/datafari-cert.pem" $KIBANA_HOME/config/kibana.yml
sed -i "/server\.key/c\server.ssl.key: ${DATAFARI_HOME}/ssl-keystore/datafari-key.pem" $KIBANA_HOME/config/kibana.yml
cd $KIBANA_HOME/bin
bash kibana
