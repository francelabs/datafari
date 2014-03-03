@echo off
export DATAFARI_HOME=%CD%\..
export JAVA_HOME=%DATAFARI_HOME%\jvm
cd %DATAFARI_HOME%\mcf\mcf_home
rm -r %DATAFARI_HOME%\pgsql\data /s /q
md %DATAFARI_HOME%\pgsql\data
sh "(echo password echo password) | %DATAFARI_HOME%\pgsql\bin\initdb -U postgres -A password --pwfile=%DATAFARI_HOME%\pgsql\pwd.conf -E utf8 -D %DATAFARI_HOME%\pgsql\data"
sh "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log start"
sh "initialize.bat"
sh "%DATAFARI_HOME%\pgsql\bin\pg_ctl -D %DATAFARI_HOME%\pgsql\data -l %DATAFARI_HOME%\logs\pgsql.log stop"
pause
