# This file is used to set custom conf to Tomcat without modifying its core files

# Add log4j2 and slf4j libs to classpath
CLASSPATH=$CATALINA_HOME/lib/log4j-api-$LOG4J_VERSION.jar:$CATALINA_HOME/lib/log4j-core-$LOG4J_VERSION.jar:$CATALINA_HOME/lib/log4j-jul-$LOG4J_VERSION.jar

# Define log4j properties file location
CATALINA_LOGGING_CONFIG="-Dlog4j.configurationFile=$TOMCAT_HOME/conf/log4j2.properties.xml"

# Define logging manager for Tomcat
LOGGING_MANAGER="-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager"

# Set CATALINA_OPTS

CATALINA_OPTS="${SOLRCLOUDOPTION} -Dorg.apache.manifoldcf.configfile=../../mcf/mcf_home/properties.xml -server -Xms1024m -Xmx1024m"
#CATALINA_OPTS="$CATALINA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,address=8696,suspend=n"

# Set redefine JAVA_OPTS
# To change the temp directory for Tomcat (by default /tmp) add this property at the end of the JAVA_OPTS property line :
# -Djava.io.tmpdir=/YOUR_FOLDER
# See https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/657620997/Change+tmp+directory+in+Datafari for more information
JAVA_OPTS="-Duser.timezone=UTC -Djava.io.tmpdir=/opt/datafari/tmp"