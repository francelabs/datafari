#!/bin/bash -e
#
#
# Factory Reset Datafari
#
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

source "${DIR}/../set-datafari-env.sh"
source "${DIR}/../utils.sh"
source $INIT_STATE_FILE
source $CONFIG_FILE


echo "This script will restore Datafato to factory setting."
echo "You will loose all your data."
echo "Datafari must be stopped"
read -p "Are you sure that you want to reinitialize COMPLETELY Datafari : all data will be lost (YES/NO) [NO]" factoryreset
factoryreset=${factoryreset:-NO}

if [ "$factoryreset" == "YES" ]; then
		cd $DIR/..
        find * -maxdepth 0 -name 'recovery_do_not_touch' -prune -o -exec rm -rf '{}' ';'
        unzip recovery_do_not_touch/datafarifactory.zip
fi
	

