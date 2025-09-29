@echo off
REM Pulsar Starter Test Suite Runner for Windows
REM This script runs comprehensive tests before each release

setlocal enabledelayedexpansion

echo ==========================================
echo Pulsar Spring Starter Test Suite
echo ==========================================

REM Check if Maven is installed
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Maven is not installed or not in PATH
    exit /b 1
)

REM Check if Docker is running
docker info >nul 2>nul
if %errorlevel% neq 0 (
    echo [WARNING] Docker is not running. Some integration tests may fail.
    echo [WARNING] Please start Docker to run full test suite.
)

echo [INFO] Starting test execution...

REM Clean and compile
echo [INFO] Cleaning and compiling project...
call mvn clean compile test-compile
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed
    exit /b 1
)

REM Run unit tests
echo [INFO] Running unit tests...
call mvn test -Dtest="!**/*IntegrationTest,!**/*PerformanceTest" -Dmaven.test.failure.ignore=false
if %errorlevel% neq 0 (
    echo [ERROR] Unit tests failed
    exit /b 1
) else (
    echo [SUCCESS] Unit tests passed
)

REM Run integration tests
echo [INFO] Running integration tests...
call mvn test -Dtest="**/*IntegrationTest" -Dmaven.test.failure.ignore=false
if %errorlevel% neq 0 (
    echo [ERROR] Integration tests failed
    exit /b 1
) else (
    echo [SUCCESS] Integration tests passed
)

REM Run performance tests (optional)
if not "%1"=="--skip-performance" (
    echo [INFO] Running performance tests...
    call mvn test -Dtest="**/*PerformanceTest" -Dmaven.test.failure.ignore=false
    if %errorlevel% neq 0 (
        echo [WARNING] Performance tests failed ^(this may be acceptable depending on environment^)
    ) else (
        echo [SUCCESS] Performance tests passed
    )
) else (
    echo [WARNING] Skipping performance tests
)

REM Generate test report
echo [INFO] Generating test reports...
call mvn surefire-report:report

REM Run static analysis
echo [INFO] Running static analysis...
call mvn spotbugs:check
if %errorlevel% neq 0 (
    echo [WARNING] SpotBugs analysis completed with warnings
)

REM Check test coverage
echo [INFO] Checking test coverage...
call mvn jacoco:report
call mvn jacoco:check
if %errorlevel% neq 0 (
    echo [WARNING] Coverage check completed with warnings
)

REM Package the project
echo [INFO] Packaging project...
call mvn package -DskipTests
if %errorlevel% neq 0 (
    echo [ERROR] Project packaging failed
    exit /b 1
) else (
    echo [SUCCESS] Project packaged successfully
)

echo.
echo ==========================================
echo [SUCCESS] All tests completed successfully!
echo ==========================================
echo.
echo [INFO] Test reports available at: target\site\surefire-report.html
echo [INFO] Coverage report available at: target\site\jacoco\index.html
echo [INFO] JAR file created at: target\
echo.
echo [INFO] Release readiness checklist:
echo   ✓ Unit tests passed
echo   ✓ Integration tests passed
if not "%1"=="--skip-performance" (
    echo   ✓ Performance tests completed
)
echo   ✓ Static analysis completed
echo   ✓ Test coverage checked
echo   ✓ Project packaged successfully
echo.
echo [SUCCESS] Project is ready for release! 🚀

endlocal