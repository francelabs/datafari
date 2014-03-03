@echo off
export DATAFARI_HOME=%CD%\..
export JAVA_HOME=%DATAFARI_HOME%\jvm
cd %DATAFARI_HOME%\mcf\mcf_home
sh "stop-agents.sh"
cd %DATAFARI_HOME%\tomcat\bin
sh "shutdown.sh"
sh "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log stop"