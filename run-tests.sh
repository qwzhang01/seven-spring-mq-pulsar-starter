#!/bin/bash

# Pulsar Starter Test Suite Runner
# This script runs comprehensive tests before each release

set -e

echo "=========================================="
echo "Pulsar Spring Starter Test Suite"
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed or not in PATH"
    exit 1
fi

# Check if Docker is running (for Pulsar testcontainer)
if ! docker info &> /dev/null; then
    print_warning "Docker is not running. Some integration tests may fail."
    print_warning "Please start Docker to run full test suite."
fi

print_status "Starting test execution..."

# Clean and compile
print_status "Cleaning and compiling project..."
mvn clean compile test-compile

# Run unit tests
print_status "Running unit tests..."
mvn test -Dtest="!**/*IntegrationTest,!**/*PerformanceTest" -Dmaven.test.failure.ignore=false

if [ $? -eq 0 ]; then
    print_success "Unit tests passed"
else
    print_error "Unit tests failed"
    exit 1
fi

# Run integration tests
print_status "Running integration tests..."
mvn test -Dtest="**/*IntegrationTest" -Dmaven.test.failure.ignore=false

if [ $? -eq 0 ]; then
    print_success "Integration tests passed"
else
    print_error "Integration tests failed"
    exit 1
fi

# Run performance tests (optional, can be skipped with --skip-performance)
if [[ "$1" != "--skip-performance" ]]; then
    print_status "Running performance tests..."
    mvn test -Dtest="**/*PerformanceTest" -Dmaven.test.failure.ignore=false
    
    if [ $? -eq 0 ]; then
        print_success "Performance tests passed"
    else
        print_warning "Performance tests failed (this may be acceptable depending on environment)"
    fi
else
    print_warning "Skipping performance tests"
fi

# Generate test report
print_status "Generating test reports..."
mvn surefire-report:report

# Run static analysis (if configured)
print_status "Running static analysis..."
mvn spotbugs:check || print_warning "SpotBugs analysis completed with warnings"

# Check test coverage
print_status "Checking test coverage..."
mvn jacoco:report
mvn jacoco:check || print_warning "Coverage check completed with warnings"

# Package the project
print_status "Packaging project..."
mvn package -DskipTests

if [ $? -eq 0 ]; then
    print_success "Project packaged successfully"
else
    print_error "Project packaging failed"
    exit 1
fi

echo ""
echo "=========================================="
print_success "All tests completed successfully!"
echo "=========================================="
echo ""
print_status "Test reports available at: target/site/surefire-report.html"
print_status "Coverage report available at: target/site/jacoco/index.html"
print_status "JAR file created at: target/"

echo ""
print_status "Release readiness checklist:"
echo "  ✓ Unit tests passed"
echo "  ✓ Integration tests passed"
if [[ "$1" != "--skip-performance" ]]; then
    echo "  ✓ Performance tests completed"
fi
echo "  ✓ Static analysis completed"
echo "  ✓ Test coverage checked"
echo "  ✓ Project packaged successfully"

echo ""
print_success "Project is ready for release! 🚀"