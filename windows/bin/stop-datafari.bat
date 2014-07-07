@echo off
set DATAFARI_HOME=%CD%\..
set JAVA_HOME=%DATAFARI_HOME%\jvm
cd %DATAFARI_HOME%\mcf\mcf_home
cmd /c "stop-agents.bat"
cd %DATAFARI_HOME%\tomcat\bin
cmd /c "shutdown.bat"
cmd /c "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log stop"