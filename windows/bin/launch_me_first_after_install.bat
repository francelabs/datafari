@echo off
set DATAFARI_HOME=%CD%\..
set JAVA_HOME=%DATAFARI_HOME%\jvm
set CATALINA_HOME=%DATAFARI_HOME%\tomcat
set CASSANDRA_HOME=%DATAFARI_HOME%\cassandra
set CASSANDRA_ENV=%CASSANDRA_HOME%\bin\cassandra.in.bat
set PYTHONPATH=%DATAFARI_HOME%\python
set PATH=%PATH%;%PYTHONPATH%
set TOMCATAPP=Bootstrap


powershell Set-ExecutionPolicy Unrestricted -Scope CurrentUser




rd "%DATAFARI_HOME%/pgsql/data" /s /q
cmd /c "%DATAFARI_HOME%\pgsql\bin\initdb -U postgres -A password --pwfile=%DATAFARI_HOME%\pgsql\pwd.conf -E utf8 -D %DATAFARI_HOME%\pgsql\data"
cmd /c "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log start"
cd "%DATAFARI_HOME%\mcf\mcf_home"
cmd /c "initialize.bat"
cmd /c %CASSANDRA_ENV%
cmd /c %CASSANDRA_HOME%\bin\cassandra 
ping 127.0.0.1 -n 10 > nul



cd %DATAFARI_HOME%\tomcat\bin
cmd /c "startup.bat"
cd "%DATAFARI_HOME%\bin\common"
cmd /c "%JAVA_HOME%\bin\java -cp DatafariScripts.jar com.francelabs.manifoldcf.configuration.script.BackupManifoldCFConnectorsScript RESTORE config\manifoldcf\monoinstance
ping 127.0.0.1 -n 10 > nul
cmd /c %CASSANDRA_HOME%\bin\cqlsh -f %DATAFARI_HOME%\bin\common\config\cassandra\tables 
cmd /c echo Creation tables Cassandra OK
cd "%DATAFARI_HOME%\bin"
cd "%DATAFARI_HOME%\tomcat\bin"
cmd /c "shutdown.bat"
ping 127.0.0.1 -n 10 > nul

cmd /c "for /f "tokens=1" %%i in ('%JAVA_HOME%\bin\jps -m ^| find "%TOMCATAPP%"') do ( taskkill /F /PID %%i )"
cmd /c "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log stop"
cmd /c %CASSANDRA_HOME%\bin\stop-server -p %CASSANDRA_HOME%\pid.txt -f


exit