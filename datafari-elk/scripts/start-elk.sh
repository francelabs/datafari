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

# Set logs path
sed -i "/path.logs/c\path.logs: ${ELK_LOGS}" $ELASTICSEARCH_HOME/config/elasticsearch.yml
sed -i "/path.logs/c\path.logs: ${ELK_LOGS}" $LOGSTASH_HOME/config/logstash.yml
sed -i "/logging.dest/c\logging.dest: ${ELK_LOGS}/kibana.log" $KIBANA_HOME/config/kibana.yml


cd $ELASTICSEARCH_HOME/bin
echo "Starting Elasticsearch..."
bash elasticsearch -p $ELASTICSEARCH_PID_FILE &
waitElasticsearch
sleep 5

# Replace the default conf with the correct paths before starting logstash
sed -i "/francelabs\/datafari-crawl.log/c\  path => \"${DATAFARI_HOME}/logs/datafari-crawl.log\"" $LOGSTASH_HOME/logstash-datafari.conf
sed -i "/francelabs\/datafari-stats.log/c\	path => \"${DATAFARI_HOME}/logs/datafari-stats.log\"" $LOGSTASH_HOME/logstash-datafari.conf
sed -i "/francelabs\/datafari-monitoring.log/c\	path => \"${DATAFARI_HOME}/logs/datafari-monitoring.log\"" $LOGSTASH_HOME/logstash-datafari.conf
sed -i "/francelabs\/datafari.log/c\	path => \"${DATAFARI_HOME}/logs/datafari.log\"" $LOGSTASH_HOME/logstash-datafari.conf
sed -i "/francelabs\/datafari-manifold.log/c\	path => \"${DATAFARI_HOME}/logs/datafari-manifold.log\"" $LOGSTASH_HOME/logstash-datafari.conf
sed -i "/francelabs\/tomcat.log/c\	path => \"${DATAFARI_HOME}/logs/tomcat.log*\"" $LOGSTASH_HOME/logstash-datafari.conf
sed -i "/francelabs\/manifoldcf.log/c\	path => \"${DATAFARI_HOME}/logs/manifoldcf.log*\"" $LOGSTASH_HOME/logstash-datafari.conf
sed -i "/francelabs\/elasticsearch.log/c\	path => \"${DATAFARI_HOME}/elk/elasticsearch/logs/elasticsearch.log*\"" $LOGSTASH_HOME/logstash-datafari.conf
sed -i "/francelabs\/solr.log/c\	path => \"${DATAFARI_HOME}/logs/solr.log*\"" $LOGSTASH_HOME/logstash-datafari.conf
sed -i "/francelabs\/zookeeper.log/c\	path => \"${DATAFARI_HOME}/logs/zookeeper.log*\"" $LOGSTASH_HOME/logstash-datafari.conf
sed -i "/francelabs\/tika-server.log/c\	path => \"${DATAFARI_HOME}/logs/tika-server.log*\"" $LOGSTASH_HOME/logstash-datafari.conf
cd $LOGSTASH_HOME
echo "Starting Logstash..."
bash bin/logstash -f $LOGSTASH_HOME/logstash-datafari.conf &
# Must sleep 1 sec to be sure to find logstash's PID
sleep 1
LOGSTASH_PID=`ps ux | grep logstash | grep runner.rb | awk '{ print $2}'`
if [ "x$LOGSTASH_PID" = "x" ]; then
LOGSTASH_PID=-1
fi
if [ $LOGSTASH_PID -ne -1 ]; then
	echo $LOGSTASH_PID > $LOGSTASH_PID_FILE
fi
echo "Logstash started !"

# Configure the right path for Kibana PID file
sed -i "/pid\.file/c\pid.file: ${KIBANA_PID_FILE}" $KIBANA_HOME/config/kibana.yml
# Uncomment to enable SSL
#sed -i "/server\.crt/c\server.ssl.cert: ${DATAFARI_HOME}/ssl-keystore/datafari-cert.pem" $KIBANA_HOME/config/kibana.yml
#sed -i "/server\.key/c\server.ssl.key: ${DATAFARI_HOME}/ssl-keystore/datafari-key.pem" $KIBANA_HOME/config/kibana.yml
cd $KIBANA_HOME/bin
export NODE_OPTIONS
echo "Starting Kibana..." 
bash kibana
