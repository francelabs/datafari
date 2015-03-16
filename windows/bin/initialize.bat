@echo off
set DATAFARI_HOME=%CD%\..
set JAVA_HOME=%DATAFARI_HOME%\jvm
rmdir "%DATAFARI_HOME%/pgsql/data" /s /q
cmd /c "%DATAFARI_HOME%/pgsql/bin/initdb -U postgres -A password --pwfile=%DATAFARI_HOME%/pgsql/pwd.conf -E utf8 -D %DATAFARI_HOME%/pgsql/data"
cmd /c "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log start"
cd "%DATAFARI_HOME%\mcf\mcf_home"
cmd /c "initialize.bat"
cd %DATAFARI_HOME%\tomcat\bin
cmd /c "startup.bat"
cd "%DATAFARI_HOME%\bin\common"
cmd /c "%JAVA_HOME%\bin\java -cp DatafariScripts.jar com.francelabs.datafari.script.BackupManifoldCFConnectorsScript RESTORE config\manifoldcf\monoinstance
cd "%DATAFARI_HOME%\bin"
cd %DATAFARI_HOME%\tomcat\bin
cmd /c "shutdown.bat"
cmd /c "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log stop"


