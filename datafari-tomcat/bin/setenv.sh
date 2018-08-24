# This file is used to set custom conf to Tomcat without modifying its core files

# Add log4j2 lib to classpath
CLASSPATH=$CATALINA_HOME/lib/log4j-api-$LOG4J_VERSION.jar:$CATALINA_HOME/lib/log4j-core-$LOG4J_VERSION.jar:$CATALINA_HOME/lib/log4j-jul-$LOG4J_VERSION.jar
# Define log4j properties file location
LOGGING_CONFIG="-Dlog4j.configurationFile=$TOMCAT_HOME/conf/log4j2.properties.xml"
# Define logging manager for Tomcat
LOGGING_MANAGER="-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager"
# Set CATALINA_OPTS
CATALINA_OPTS="${SOLRCLOUDOPTION} -Dorg.apache.manifoldcf.configfile=../../mcf/mcf_home/properties.xml -server -Xms1024m -Xmx1024m"
