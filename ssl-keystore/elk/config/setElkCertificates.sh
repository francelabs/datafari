#!/bin/bash -e
#
#
# Set SSL certificates for Apache
#
#

pathCertificates=/opt/datafari/ssl-keystore/customerCertificates
certificatefile=${pathCertificates}/certificate.crt
certificatekeyfile=${pathCertificates}/certificateKey.key


elasticsearchConfFolder=/opt/datafari/elk/elasticsearch/config
defaultCertificateName=datafari-cert.pem
defaultCertificateKey=datafari-key.pem


cp $certificatefile $elasticsearchConfFolder/$defaultCertificateName
cp $certificatekeyfile $elasticsearchConfFolder/$defaultCertificateKey

