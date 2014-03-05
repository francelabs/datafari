@echo off
set DATAFARI_HOME=%CD%\..
set JAVA_HOME=%DATAFARI_HOME%\jvm
cd %DATAFARI_HOME%\mcf\mcf_home
rmdir %DATAFARI_HOME%\pgsql\data /s /q
mkdir %DATAFARI_HOME%\pgsql\data
cmd /c "%DATAFARI_HOME%\pgsql\bin\initdb -U postgres -A password --pwfile=%DATAFARI_HOME%\pgsql\pwd.conf -E utf8 -D %DATAFARI_HOME%\pgsql\data"
cmd /c "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log start"
cmd /c "initialize.bat"
cmd /c "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log stop"
pause
