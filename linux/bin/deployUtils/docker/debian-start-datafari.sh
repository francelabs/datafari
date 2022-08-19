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
# For Enterprise Edition only
GLOBAL_MONITOR_SCRIPT_FILE=/opt/datafari/bin/monitorUtils/global_monitor_script.sh
if test -f "$GLOBAL_MONITOR_SCRIPT_FILE"; then
  sed -i -e '/^[[:space:]]*#Start Debian Glances conf/,/^[[:space:]]*#End Debian Glances conf/{/^[[:space:]]*#Start Debian Glances conf/!{/^[[:space:]]*#End Debian Glances conf/!d}}' $GLOBAL_MONITOR_SCRIPT_FILE
  glancesDockerSection='  cp $MONITOR_PATH/$MONIT_FILE /etc/monit/\n  cp $MONITOR_PATH/glances.conf /etc/glances/glances.conf\n  cp $MONITOR_PATH/glances.conf /etc/glances/glances.conf\n  chown root:root /etc/monit/monitrc\n  chmod 700 /etc/monit/monitrc\n  /usr/local/bin/glances -w &'
  sed -i -e "/^[[:space:]]*#Start Debian Glances conf.*/a${glancesDockerSection}" $GLOBAL_MONITOR_SCRIPT_FILE
fi
cd /opt/datafari/bin && bash start-datafari.sh
sleep infinity