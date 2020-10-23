#!/bin/bash

cp /opt/datafari/bin/deployUtils/docker/datafari.properties /opt/datafari/tomcat/conf/datafari.properties
cd /opt/datafari/bin && bash init-datafari.sh
rm -rf /opt/datafari/apache/sites-available/tomcat.conf
cp /opt/datafari/bin/deployUtils/docker/tomcat.conf /opt/datafari/apache/sites-available/tomcat.conf
cd /opt/datafari/bin && bash start-datafari.sh  &
exec /usr/sbin/init
