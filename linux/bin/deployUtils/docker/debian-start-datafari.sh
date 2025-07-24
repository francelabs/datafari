#!/bin/bash -e
#
#
# Startup script for Datafari

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/../../set-datafari-env.sh"
source "${DIR}/../../utils.sh"

source /opt/datafari/bin/common/init_state.properties

cd /opt/datafari/bin/monitorUtils
bash monit-start-zk-mcf.sh

echo "[INFO] Attempting to clean up ZooKeeper locks for Agent A..."

cat <<EOF > /tmp/zk_cleanup.cmd
delete /org.apache.manifoldcf/service-AGENT/child-A
delete /org.apache.manifoldcf/service-AGENT
delete /org.apache.manifoldcf/servicelock-AGENT
delete /org.apache.manifoldcf/servicelock-AGENT_org.apache.manifoldcf.crawler.system.CrawlerAgent
delete /org.apache.manifoldcf/serviceactive-AGENT-A
EOF

/opt/datafari/zookeeper-mcf/bin/zkCli.sh -server localhost:2182 < /tmp/zk_cleanup.cmd

rm -f /tmp/zk_cleanup.cmd

cd /opt/datafari/bin/monitorUtils
bash monit-stop-zk-mcf.sh


cp /opt/datafari/bin/deployUtils/docker/datafari.properties /opt/datafari/tomcat/conf/datafari.properties


if  [[ "$STATE" != *active* ]];
then
    cd /opt/datafari/bin && bash init-datafari.sh
	if [ $# -eq 0 ]; then
		rm -rf /opt/datafari/apache/sites-available/tomcat.conf
		if [[ "$DATAFARIUIDEV" == true ]]
		then
		  cp /opt/datafari/bin/deployUtils/docker/tomcat-datafariuidev.conf /opt/datafari/apache/sites-available/tomcat.conf
		else
		  cp /opt/datafari/bin/deployUtils/docker/tomcat.conf /opt/datafari/apache/sites-available/tomcat.conf
        fi
        chown datafari /opt/datafari/apache/sites-available/tomcat.conf
        chmod 755 /opt/datafari/apache/sites-available/tomcat.conf
	fi

fi

cd /opt/datafari/bin && bash start-datafari.sh

sleep infinity

