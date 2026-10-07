@echo off
setlocal
if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Java\jdk-22" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-22"
    )
)
echo [Cyber Heist] Starting game...
if exist "target\cyber-heist-0.1.0-SNAPSHOT.jar" (
    java -jar "target\cyber-heist-0.1.0-SNAPSHOT.jar"
) else (
    call .\mvnw.cmd compile exec:java
)
endlocal
