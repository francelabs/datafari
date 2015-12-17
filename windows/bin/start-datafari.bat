@echo off
set DATAFARI_HOME=%CD%\..
set JAVA_HOME=%DATAFARI_HOME%\jvm
set CASSANDRA_HOME=%DATAFARI_HOME%\cassandra
set CASSANDRA_ENV=%CASSANDRA_HOME%\bin\cassandra.in.bat
set SOLR_INSTALL_DIR=%DATAFARI_HOME%\solr
set SOLR_ENV=%SOLR_INSTALL_DIR%\bin\solr.in.cmd

powershell Set-ExecutionPolicy Unrestricted -Scope CurrentUser


cmd /c %CASSANDRA_HOME%\bin\cassandra

cmd /c "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log start"
cmd /c %SOLR_INSTALL_DIR%\bin\solr start
cd %DATAFARI_HOME%\tomcat\bin
cmd /c "startup.bat"
cd %DATAFARI_HOME%\mcf\mcf_home
cmd /c "lock-clean.bat"
cmd /c "start-agents.bat"