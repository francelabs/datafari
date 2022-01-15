#!/bin/bash -e
#
#
# Monitor MCF jobs and restart it if necessary
#
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source "${DIR}/../set-datafari-env.sh"
source "${DIR}/../utils.sh"
source $CONFIG_FILE

JOB_IDS_TO_MONITOR="ALL"
IP_MCF=http://localhost:9080


echo "Check the MCF job(s) status"

if [ "$PASSWORD_MCF" = "@MCF_ADMIN_PASSWORD@" ]
then
	echo "You must launch the script global_monitor_script.sh before using this script to change the password"
	
fi

curl -c "cookie_mcfscript" -XPOST ${IP_MCF}/datafari-mcf-api-service/json/LOGIN -d '{ "userID":"","password":"'"${MCFPASSWORD}"'" }'

JOBS_EXIST=$(curl -b "cookie_mcfscript" -s $IP_MCF'/datafari-mcf-api-service/json/jobstatuses')

if [ "$JOBS_EXIST" = "{}" ]
then
      echo "No job configured"
      echo "The script will exit"
      exit 0
else



JOB_IDS=$(curl -b "cookie_mcfscript" -s $IP_MCF'/datafari-mcf-api-service/json/jobstatuses' | jq -r '.jobstatus | if type=="array" then .[]._children_[] else ._children_[] end | select(._type_ == "job_id") | ._value_')
echo $JOB_IDS
jobstatus="ok"


for id in $JOB_IDS; do
        STATUS=$(curl -b "cookie_mcfscript" -s $IP_MCF'/datafari-mcf-api-service/json/jobstatuses/'$id | jq -r '.jobstatus | ._children_[] | select(._type_ == "status") | ._value_')
echo $STATUS      
        if [ "$STATUS" = "running" ] || [ "$STATUS" = "stopping" ] || [ "$STATUS" = "resuming" ] || [ "$STATUS" = "starting up" ] || [ "$STATUS" = "cleaning up" ] || [ "$STATUS" = "aborting" ] || [ "$STATUS" = "restarting" ] || [ "$STATUS" = "terminating" ]; then
                #curl -XPUT $IP_MCF'/datafari-mcf-api-service/json/stop/'$id
                #echo "stop job $id"
                echo "One of the MCF jobs has this status $STATUS" 
                jobstatus="ko"
                exit 1
                
                
        fi
done

fi

