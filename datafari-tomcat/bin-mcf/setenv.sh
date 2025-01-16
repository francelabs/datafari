# This file is used to set custom conf to Tomcat without modifying its core files

# Add log4j2 and slf4j libs to classpath
CLASSPATH=$MCF_HOME/lib/log4j-api-$LOG4J_VERSION.jar:$MCF_HOME/lib/log4j-core-$LOG4J_VERSION.jar:$MCF_HOME/lib/log4j-jul-$LOG4J_VERSION.jar

# Define log4j properties file location
LOGGING_CONFIG="-Dlog4j.configurationFile=$TOMCAT_MCF_HOME/conf/log4j2.properties.xml"

# Define logging manager for Tomcat
LOGGING_MANAGER="-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager"

# Set CATALINA_OPTS
CATALINA_OPTS="-Dorg.apache.manifoldcf.configfile=../../mcf/mcf_home/properties.xml -server -Xms@TOMCATMCFMEMORY@ -Xmx@TOMCATMCFMEMORY@ -Djute.maxbuffer=10000000"
#CATALINA_OPTS="$CATALINA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,address=8697,suspend=n"

# Set redefine JAVA_OPTS
# To change the temp directory for Tomcat (by default /tmp) add this property at the end of the JAVA_OPTS property line :
# -Djava.io.tmpdir=/YOUR_FOLDER
# See https://datafari.atlassian.net/wiki/spaces/DATAFARI/pages/657620997/Change+tmp+directory+in+Datafari for more information
JAVA_OPTS="-Duser.timezone=UTC -Djava.io.tmpdir=@TOMCATMCFTMPDIR@"


CATALINA_PID="$CATALINA_MCF_PID"