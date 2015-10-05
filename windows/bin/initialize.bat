:: BatchGotAdmin
:-------------------------------------
REM  --> Check for permissions
>nul 2>&1 "%SYSTEMROOT%\system32\cacls.exe" "%SYSTEMROOT%\system32\config\system"

REM --> If error flag set, we do not have admin.
if '%errorlevel%' NEQ '0' (
    echo Requesting administrative privileges...
    goto UACPrompt
) else ( goto gotAdmin )

:UACPrompt
    echo Set UAC = CreateObject^("Shell.Application"^) > "%temp%\getadmin.vbs"
    set params = %*:"=""
    echo UAC.ShellExecute "cmd.exe", "/c %~s0 %params%", "", "runas", 1 >> "%temp%\getadmin.vbs"

    "%temp%\getadmin.vbs"
    del "%temp%\getadmin.vbs"
    exit /B

:gotAdmin
    pushd "%CD%"
    CD /D "%~dp0"
:--------------------------------------



@echo off
set DATAFARI_HOME=%CD%\..
set JAVA_HOME=%DATAFARI_HOME%\jvm
set CASSANDRA_HOME=%DATAFARI_HOME%\cassandra
set CASSANDRA_ENV=%CASSANDRA_HOME%\bin\cassandra.in.bat
set TOMCATAPP=Bootstrap


powershell Set-ExecutionPolicy Unrestricted




rd "%DATAFARI_HOME%/pgsql/data" /s /q
cmd /c "%DATAFARI_HOME%\pgsql\bin\initdb -U postgres -A password --pwfile=%DATAFARI_HOME%\pgsql\pwd.conf -E utf8 -D %DATAFARI_HOME%\pgsql\data"
cmd /c "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log start"
cd "%DATAFARI_HOME%\mcf\mcf_home"
cmd /c "initialize.bat"
cmd /c start /b %CASSANDRA_HOME%\bin\cassandra 
cmd /c ping 192.168.0.1 -n 1 -w 5000 > nul
cmd /c %CASSANDRA_HOME%\bin\cqlsh -f %DATAFARI_HOME%\bin\common\config\cassandra\tables 
cmd /c %CASSANDRA_HOME%\bin\cqlsh -f %DATAFARI_HOME%\bin\common\config\cassandra\create-admin-dev 


cd %DATAFARI_HOME%\tomcat\bin
cmd /c "startup.bat"
cd "%DATAFARI_HOME%\bin\common"
cmd /c "%JAVA_HOME%\bin\java -cp DatafariScripts.jar com.francelabs.datafari.script.BackupManifoldCFConnectorsScript RESTORE config\manifoldcf\monoinstance

cd "%DATAFARI_HOME%\bin"
cd "%DATAFARI_HOME%\tomcat\bin"
cmd /c ping 192.168.0.1 -n 1 -w 5000 > nul
cmd /c "shutdown.bat"


cmd /c "for /f "tokens=1" %%i in ('jps -m ^| find "%TOMCATAPP%"') do ( taskkill /F /PID %%i )"
cmd /c "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log stop"
cmd /c %CASSANDRA_HOME%\bin\stop-server -p %CASSANDRA_HOME%\pid.txt -f
