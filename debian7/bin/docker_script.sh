#!/bin/bash -e
#
#
# Deployment script for Datafari
#
#



mkdir -p /opt/datafari/tomcat/webapps/Datafari
mv /opt/datafari/tomcat/webapps/Datafari.war /opt/datafari/tomcat/webapps/Datafari
unzip -qq /opt/datafari/tomcat/webapps/Datafari/Datafari.war -d /opt/datafari/tomcat/webapps/Datafari

# Get Git commit id and version 

 
 file="/opt/datafari/tomcat/conf/git.properties"

if [ -f "$file" ]
	then
		while IFS='=' read -r key value
  		do
			if  [[ $key == git.commit.id.abbrev ]]
			then
			
				commit=$value
				
			fi
			
			if  [[ $key == git.closest.tag.name ]]
			then
			
				version=$value
			fi
  		done < "$file"
fi

chmod -R 755 /opt/datafari
sed -i -e "s/@VERSION@/$version/g" /opt/datafari/tomcat/webapps/Datafari/footer.jsp
sed -i -e "s/@VERSION@/$version/g" /opt/datafari/tomcat/webapps/Datafari/admin/admin-footer.jsp
sed -i -e "s/@COMMIT@/$commit/g" /opt/datafari/tomcat/webapps/Datafari/admin/admin-footer.jsp


#start script

#change elk address
sed -i "/server.host:/c\server.host: 0.0.0.0" /opt/datafari/elk/kibana/config/kibana.yml
sed -i -e "s/@NODEHOST@/localhost/g" /opt/datafari/bin/utils.sh

sed -i -e "s/@NODEHOST@/localhost/g" /opt/datafari/tomcat/conf/datafari.properties
sed -i -e "s/@NODEHOST@/localhost/g" /opt/datafari/solr/bin/solr.in.sh
sed -i -e "s/@SOLRHOSTS@/localhost:2181/g" /opt/datafari/solr/bin/solr.in.sh
sed -i -e "s/@SOLRHOSTS@/localhost:2181/g" /opt/datafari/tomcat/conf/datafari.properties
sed -i -e "s/@ZKHOST@/localhost:2181/g" /opt/datafari/mcf/mcf_home/properties.xml

sed -i -e "s/@NUMSHARDS@/1/g" /opt/datafari/tomcat/conf/datafari.properties
sed -i -e "s/@NUMSHARDS@/1/g" /opt/datafari/bin/start-datafari.sh
sed -i -e "s/@ISMAINNODE@/true/g" /opt/datafari/tomcat/conf/datafari.properties

mkdir -p /opt/datafari/solr/solrcloud
mv /opt/datafari/solr/solr_home/FileShare /opt/datafari/solr/solrcloud
mv /opt/datafari/solr/solr_home/Statistics /opt/datafari/solr/solrcloud
mv /opt/datafari/solr/solr_home/Promolink /opt/datafari/solr/solrcloud

cd /opt/datafari/mcf/mcf_home/obfuscation-utility
sed -i -e "s~@PASSWORD@~$(./obfuscate.sh admin)~g" /opt/datafari/mcf/mcf_home/properties-global.xml
sed -i -e "s/@TEMPADMINPASSWORD@/admin/g" /opt/datafari/tomcat/conf/datafari.properties
sed -i -e "s~@POSTGRESPASSWORD@~$(./obfuscate.sh admin)~g" /opt/datafari/mcf/mcf_home/properties-global.xml
sed -i -e "s~@POSTGRESPASSWORD@~admin~g" /opt/datafari/pgsql/pwd.conf
id -u postgres >/dev/null 2>&1 || useradd postgres
id -u datafari >/dev/null 2>&1 || useradd datafari
chown -R datafari /opt/datafari
chown -R postgres /opt/datafari/pgsql/
chmod -R 700 /opt/datafari/pgsql/
chmod -R 777 /opt/datafari/pid
chmod -R 777 /opt/datafari/logs
chmod 755 /opt/datafari/elk/kibana/bin/*
chmod 755 /opt/datafari/elk/kibana/node/bin/*
chmod 755 /opt/datafari/elk/logstash/vendor/jruby/bin/*
chmod 755 /opt/datafari/elk/scripts/*
