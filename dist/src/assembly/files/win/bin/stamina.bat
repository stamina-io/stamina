@echo off
setlocal

rem Copyright (c) 2017 Stamina developers.
rem All rights reserved.

set "STAMINA_HOME=%~dp0
set "STAMINA_HOME=%STAMINA_HOME:~0,-1%\.."

rem Run this command if you want to debug this process:
rem $ set STAMINA_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000

java %STAMINA_OPTS% ^
    "-Dstamina.home=%STAMINA_HOME%" ^
    "-Djava.util.logging.config.file=%STAMINA_HOME%\etc\java.util.logging.properties" ^
    -cp "%STAMINA_HOME%\lib\*" ^
    io.stamina.boot.Main ^
    %*
