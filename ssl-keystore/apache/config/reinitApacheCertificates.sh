#!/bin/bash -e
#
#
# Reinit SSL certificates for Apache
#
#

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source "${DIR}/../../../bin/set-datafari-env.sh"
source "${DIR}/../../../bin/utils.sh"


if [ "$(whoami)" != "$DATAFARI_USER" ]; then
        echo "Script must be run as user: $DATAFARI_USER"
        echo "Script will exit"
        exit 1
fi



defaultpathCertificates=${DIR}/..
certificatefile=${defaultpathCertificates}/datafari.crt
certificatekeyfile=${defaultpathCertificates}/datafari.key

defaultcertificatefile=${defaultpathCertificates}/backup/datafari.crt
defaultcertificatekeyfile=${defaultpathCertificates}/backup/datafari.key
echo "Change of default Apache certificates"

cp $defaultcertificatefile $certificatefile 
cp $defaultcertificatekeyfile $certificatekeyfile

echo "Certificates changed to default. Restart of Apache needed"
#cd /opt/datafari/bin/monitorUtils
#bash monit-stop-apache.sh
#bash monit-start-apache.sh
#echo "Restart done"
