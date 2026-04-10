# This file is used to set custom conf to Tomcat without modifying its core files

# Define log4j properties file location
CATALINA_LOGGING_CONFIG="-Dlog4j.configurationFile=$TOMCAT_HOME/conf/log4j2.properties.xml"

# Set CATALINA_OPTS

CATALINA_OPTS="${SOLRCLOUDOPTION} -Dorg.apache.manifoldcf.configfile=../../mcf/mcf_home/properties.xml -server -Xms@TOMCATMEMORY@ -Xmx@TOMCATMEMORY@ -Djute.maxbuffer=10000000"
#CATALINA_OPTS="$CATALINA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,address=8696,suspend=n"

# Set redefine JAVA_OPTS
# To change the temp directory for Tomcat (by default /tmp) add this property at the end of the JAVA_OPTS property line :
# -Djava.io.tmpdir=/YOUR_FOLDER
# See https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/657620997/Change+tmp+directory+in+Datafari for more information
JAVA_OPTS="-Duser.timezone=UTC -Djava.io.tmpdir=@TOMCATTMPDIR@"