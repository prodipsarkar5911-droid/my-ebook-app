@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem  Gradle startup script for Windows
@rem ##########################################################################
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%
java -version
"%JAVA_HOME%\bin\java.exe" -version
java -jar "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" %*
