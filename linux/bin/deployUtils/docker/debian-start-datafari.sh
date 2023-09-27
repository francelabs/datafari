#!/bin/bash -e
#
#
# Startup script for Datafari

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/../../set-datafari-env.sh"
source "${DIR}/../../utils.sh"

source /opt/datafari/bin/common/init_state.properties


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

