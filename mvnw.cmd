@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script, version 3.3.4
@REM ----------------------------------------------------------------------------
@echo off
setlocal
set "MAVEN_PROJECTBASEDIR=%~dp0"
if not "%MAVEN_PROJECTBASEDIR:~-1%" == "\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR%\"
set "MAVEN_PROJECTBASEDIR_NO_TRAILING=%MAVEN_PROJECTBASEDIR:~0,-1%"
set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
if not exist "%WRAPPER_JAR%" (
  echo Downloading Maven Wrapper 3.3.4...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar' -OutFile '%WRAPPER_JAR%'"
  if errorlevel 1 exit /b 1
)
if defined JAVA_HOME (
  "%JAVA_HOME%\bin\java.exe" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR_NO_TRAILING%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
) else (
  java.exe -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR_NO_TRAILING%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
)
set "WRAPPER_EXIT=%ERRORLEVEL%"
endlocal & exit /b %WRAPPER_EXIT%
