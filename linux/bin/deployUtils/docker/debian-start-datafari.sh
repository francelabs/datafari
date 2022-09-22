#!/bin/bash -e
#
#
# Startup script for Datafari

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/../../set-datafari-env.sh"
source "${DIR}/../../utils.sh"

cp /opt/datafari/bin/deployUtils/docker/datafari.properties /opt/datafari/tomcat/conf/datafari.properties

source /opt/datafari/bin/common/init_state.properties
folderEmpty="true"
DIRBACKUPUTILS="/opt/datafari/bin/backupUtils"
enterprise="false"

[ -d "/opt/datafari/bin/backupUtils" ] && enterprise="true"


check_subfolders(){

subdircount=$(find $1 -maxdepth 1 -type d | wc -l)


if [[ "$subdircount" -eq 1 ]]
then
    folderEmpty="true"

else
    folderEmpty="false"
fi

}

if  [[ "$STATE" != *active* ]];
then
	firstStart="true"
        cd /opt/datafari/bin && bash init-datafari.sh
	if [ $# -eq 0 ]; then
		rm -rf /opt/datafari/apache/sites-available/tomcat.conf
		cp /opt/datafari/bin/deployUtils/docker/tomcat.conf /opt/datafari/apache/sites-available/tomcat.conf
                chown datafari /opt/datafari/apache/sites-available/tomcat.conf
                chmod 755 /opt/datafari/apache/sites-available/tomcat.conf
	fi
	# For Enterprise Edition only
	GLOBAL_MONITOR_SCRIPT_FILE=/opt/datafari/bin/monitorUtils/global_monitor_script.sh
		if test -f "$GLOBAL_MONITOR_SCRIPT_FILE"; then
  			sed -i -e '/^[[:space:]]*#Start Debian Glances conf/,/^[[:space:]]*#End Debian Glances conf/{/^[[:space:]]*#Start Debian Glances conf/!{/^[[:space:]]*#End Debian Glances conf/!d}}' $GLOBAL_MONITOR_SCRIPT_FILE
 			glancesDockerSection='  cp $MONITOR_PATH/$MONIT_FILE /etc/monit/\n  cp $MONITOR_PATH/glances.conf /etc/glances/glances.conf\n  cp $MONITOR_PATH/glances.conf /etc/glances/glances.conf\n  chown root:root /etc/monit/monitrc\n  chmod 700 /etc/monit/monitrc\n  /usr/local/bin/glances -w &'
  			sed -i -e "/^[[:space:]]*#Start Debian Glances conf.*/a${glancesDockerSection}" $GLOBAL_MONITOR_SCRIPT_FILE
		fi

fi


cd /opt/datafari/bin && bash start-datafari.sh

if [[ "$firstStart" == true ]] && [[ "$enterprise" == true ]]
then
  echo "Check if volume present with data that need to be restored"
  check_subfolders /opt/datafari/bin/backup/cassandra
  if [[ "$folderEmpty" == *false* ]]
  then
   cd $DIRBACKUPUTILS
   run_as ${DATAFARI_USER} "bash restore_cassandra.sh";
  fi

  check_subfolders /opt/datafari/bin/backup/datafari_conf
  if [[ "$folderEmpty" == *false* ]]
  then
   cd $DIRBACKUPUTILS
   run_as ${DATAFARI_USER} "bash restore_datafari_conf.sh";
  fi

  check_subfolders /opt/datafari/bin/backup/mcf-script
  if [[ "$folderEmpty" == *false* ]]
  then
   cd $DIRBACKUPUTILS
   run_as ${DATAFARI_USER} "bash restore_mcf.sh";
  fi

  check_subfolders /opt/datafari/bin/backup/solr
  if [[ "$folderEmpty" == *false* ]]
  then
   cd $DIRBACKUPUTILS
   run_as ${DATAFARI_USER} "bash restore_solr.sh full";
  fi

echo "end restore check"
fi
sleep infinity

