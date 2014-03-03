@echo off
export DATAFARI_HOME=%CD%\..
export JAVA_HOME=%DATAFARI_HOME%\jvm
sh "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log start"
cd %DATAFARI_HOME%\tomcat\bin
sh "startup.sh"
cd %DATAFARI_HOME%\mcf\mcf_home
sh "lock-clean.sh"
sh "start-agents.sh"