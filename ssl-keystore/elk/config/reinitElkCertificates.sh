#!/bin/bash -e
#
#
# Set SSL certificates for Apache
#
#



elasticsearchConfFolder=/opt/datafari/elk/elasticsearch/config
elkCertificateName=datafari-cert.pem
elkCertificateKey=datafari-key.pem


backupPathCertificates=/opt/datafari/ssl-keystore/elk/backup
certificatefile=${backupPathCertificates}/datafari-cert.pem
certificatekeyfile=${backupPathCertificates}/datafari-key.pem




cp $certificatefile $elasticsearchConfFolder/$elkCertificateName 
cp $certificatekeyfile $elasticsearchConfFolder/$elkCertificateKey
