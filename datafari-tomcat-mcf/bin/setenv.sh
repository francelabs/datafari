# This file is used to set custom conf to Tomcat without modifying its core files

# Add log4j2 and slf4j libs to classpath
CLASSPATH=$CATALINA_HOME/lib/log4j-api-$LOG4J_VERSION.jar:$CATALINA_HOME/lib/log4j-core-$LOG4J_VERSION.jar:$CATALINA_HOME/lib/log4j-jul-$LOG4J_VERSION.jar:$CATALINA_HOME/lib/log4j-slf4j-impl-$LOG4J_VERSION.jar:$CATALINA_HOME/lib/log4j-jcl-$LOG4J_VERSION.jar:$CATALINA_HOME/lib/slf4j-api-$SLF4J_VERSION.jar:$CATALINA_HOME/lib/commons-logging-$COMMONS_LOGGING_VERSION.jar

# Define log4j properties file location
LOGGING_CONFIG="-Dlog4j.configurationFile=$TOMCAT_MCF_HOME/conf/log4j2.properties.xml"

# Define logging manager for Tomcat
LOGGING_MANAGER="-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager"

# Set CATALINA_OPTS
CATALINA_OPTS="-Dorg.apache.manifoldcf.configfile=../../mcf/mcf_home/properties.xml -server -Xms1024m -Xmx1024m"

# Set redefine JAVA_OPTS
JAVA_OPTS="-Duser.timezone=UTC"


CATALINA_PID="$CATALINA_MCF_PID"