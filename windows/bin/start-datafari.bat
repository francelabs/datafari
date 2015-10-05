

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
    exit

:gotAdmin
    pushd "%CD%"
    CD /D "%~dp0"
:--------------------------------------

@echo off
set DATAFARI_HOME=%CD%\..
set JAVA_HOME=%DATAFARI_HOME%\jvm
set CASSANDRA_HOME=%DATAFARI_HOME%\cassandra
set CASSANDRA_ENV=%CASSANDRA_HOME%\bin\cassandra.in.bat
set SOLR_INSTALL_DIR=%DATAFARI_HOME%\solr
set SOLR_ENV=%SOLR_INSTALL_DIR%\bin\solr.in.cmd

cmd /c %CASSANDRA_HOME%\bin\cassandra

cmd /c "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log start"
cmd /c %SOLR_INSTALL_DIR%\bin\solr start
cd %DATAFARI_HOME%\tomcat\bin
cmd /c "startup.bat"
cd %DATAFARI_HOME%\mcf\mcf_home
cmd /c "lock-clean.bat"
cmd /c "start-agents.bat"