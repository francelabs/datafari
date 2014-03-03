@echo off
set DATAFARI_HOME=%CD%\..
set JAVA_HOME=%DATAFARI_HOME%\jvm
cmd /c "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log start"
cd %DATAFARI_HOME%\tomcat\bin
cmd /c "startup.bat"
cd %DATAFARI_HOME%\mcf\mcf_home
cmd /c "lock-clean.bat"
cmd /c "start-agents.bat"