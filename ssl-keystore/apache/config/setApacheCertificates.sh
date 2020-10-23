#!/bin/bash -e
#
#
# Set SSL certificates for Apache
#
#
pathCertificates=/opt/datafari/ssl-keystore/customerCertificates
apacheConfFile=/opt/datafari/apache/sites-available/tomcat.conf
certificatefile=${pathCertificates}/certificate.crt
certificatekeyfile=${pathCertificates}/certificateKey.key

sed -i "s|\(SSLCertificateFile *\).*|\1${certificatefile}|" ${apacheConfFile}
sed -i "s|\(SSLCertificateKeyFile *\).*|\1${certificatekeyfile}|" ${apacheConfFile}

if [ -d /etc/apache2 ]; then
	/etc/init.d/apache2 stop
    /etc/init.d/apache2 start
elif [ -d /etc/httpd ]; then
	apachectl stop
    apachectl start
fi
