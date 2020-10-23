#!/bin/bash -e
#
#
# Restart script for Datafari
#
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source "${DIR}/set-datafari-env.sh"
source "${DIR}/utils.sh"
source $CONFIG_FILE

echo "Restart of Datafari"
errors=""
# TODO add variable for check if Monit is installed

@VERSION-RESTART@

#monoserver case
if [ "$NODETYPE" != "main" ]; then
    echo "Check status of MCF jobs"
    cd $DIR/monitorUtils/
    bash check_jobs_mcf.sh 
    if [ $? -eq 1 ]; then
        echo "MCF jobs still active, please stop or pause them before restarting Datafari"
        echo "END OF SCRIPT"
        exit 1
    fi
    sleep 10
    cd $DIR
    echo "1/2 Stop Datafari"
    if [ "$MONIT_STATE" == "active" ]; then
        sudo service monit stop
    fi
    sleep 2
    bash stop-datafari.sh
    echo "2/2 Start Datafari"
    sleep 2
    bash start-datafari.sh
    if [ "$MONIT_STATE" == "active" ]; then
        sudo service monit start
    fi
fi
if [ -z "$errors" ]
then
    echo $errors
fi
echo "END OF SCRIPT"


