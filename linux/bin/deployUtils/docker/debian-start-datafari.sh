#!/bin/bash -e
#
#
# Startup script for Datafari
cp /opt/datafari/bin/deployUtils/docker/datafari.properties /opt/datafari/tomcat/conf/datafari.properties
cd /opt/datafari/bin && bash init-datafari.sh
if [ $# -eq 0 ]; then
	rm -rf /opt/datafari/apache/sites-available/tomcat.conf
	cp /opt/datafari/bin/deployUtils/docker/tomcat.conf /opt/datafari/apache/sites-available/tomcat.conf
fi
cd /opt/datafari/bin && bash start-datafari.sh
sleep infinity