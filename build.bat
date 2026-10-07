@echo off
setlocal
if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Java\jdk-22" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-22"
    )
)
echo [Cyber Heist] Compiling and packaging project...
call .\mvnw.cmd clean package
endlocal
