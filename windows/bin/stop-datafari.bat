@echo off

set DATAFARI_HOME=%CD%\..
set JAVA_HOME=%DATAFARI_HOME%\jvm
set CASSANDRA_HOME=%DATAFARI_HOME%\cassandra
set CASSANDRA_ENV=%CASSANDRA_HOME%\bin\cassandra.in.bat
set PYTHONPATH=%DATAFARI_HOME%\python
set PATH=%PATH%;%PYTHONPATH%
set SOLR_INSTALL_DIR=%DATAFARI_HOME%\solr
set SOLR_ENV=%SOLR_INSTALL_DIR%\bin\solr.in.cmd
set TOMCATAPP=Bootstrap



cd %DATAFARI_HOME%\mcf\mcf_home
cmd /c "stop-agents.bat"
cd %DATAFARI_HOME%\tomcat\bin
cmd /c "shutdown.bat"
cmd /c "for /f "tokens=1" %%i in ('%JAVA_HOME%\bin\jps -m ^| find "%TOMCATAPP%"') do ( taskkill /F /PID %%i )"

cmd /c "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log stop"
cmd /c %CASSANDRA_HOME%\bin\stop-server -p %CASSANDRA_HOME%\pid.txt -f
cmd /c %SOLR_INSTALL_DIR%\bin\solr stop
exit