#!/bin/bash

# Test Environment Verification Script
# This script verifies that the test environment is properly configured

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[CHECK]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[⚠]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

echo "=========================================="
echo "Test Environment Verification"
echo "=========================================="

# Check Java version
print_status "Checking Java version..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    JAVA_MAJOR=$(echo $JAVA_VERSION | cut -d'.' -f1)
    if [ "$JAVA_MAJOR" -ge 11 ]; then
        print_success "Java $JAVA_VERSION (✓ Java 11+ required)"
    else
        print_error "Java $JAVA_VERSION (✗ Java 11+ required)"
        exit 1
    fi
else
    print_error "Java not found in PATH"
    exit 1
fi

# Check Maven version
print_status "Checking Maven version..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1 | cut -d' ' -f3)
    print_success "Maven $MVN_VERSION"
else
    print_error "Maven not found in PATH"
    exit 1
fi

# Check Docker
print_status "Checking Docker..."
if command -v docker &> /dev/null; then
    if docker info &> /dev/null; then
        DOCKER_VERSION=$(docker --version | cut -d' ' -f3 | cut -d',' -f1)
        print_success "Docker $DOCKER_VERSION (running)"
    else
        print_warning "Docker installed but not running"
        print_warning "Integration tests may fail without Docker"
    fi
else
    print_warning "Docker not found"
    print_warning "Integration tests will be skipped"
fi

# Check project structure
print_status "Checking project structure..."
if [ -f "pom.xml" ]; then
    print_success "pom.xml found"
else
    print_error "pom.xml not found - are you in the project root?"
    exit 1
fi

if [ -d "src/main/java" ]; then
    print_success "Source directory found"
else
    print_error "Source directory not found"
    exit 1
fi

if [ -d "src/test/java" ]; then
    print_success "Test directory found"
else
    print_error "Test directory not found"
    exit 1
fi

# Check test configuration
print_status "Checking test configuration..."
if [ -f "src/test/resources/application-test.yml" ]; then
    print_success "Test configuration found"
else
    print_warning "Test configuration not found"
fi

# Check test dependencies
print_status "Checking test dependencies..."
mvn dependency:resolve -Dclassifier=test-jar &> /dev/null
if [ $? -eq 0 ]; then
    print_success "Test dependencies resolved"
else
    print_error "Failed to resolve test dependencies"
    exit 1
fi

# Check if tests can compile
print_status "Checking test compilation..."
mvn test-compile &> /dev/null
if [ $? -eq 0 ]; then
    print_success "Tests compile successfully"
else
    print_error "Test compilation failed"
    exit 1
fi

# Quick smoke test
print_status "Running quick smoke test..."
mvn test -Dtest="PulsarStarterTestSuite" &> /dev/null
if [ $? -eq 0 ]; then
    print_success "Smoke test passed"
else
    print_warning "Smoke test failed (this may be normal if Pulsar is not running)"
fi

echo ""
echo "=========================================="
print_success "Environment verification completed!"
echo "=========================================="
echo ""
print_status "Summary:"
echo "  ✓ Java environment ready"
echo "  ✓ Maven configured"
if docker info &> /dev/null; then
    echo "  ✓ Docker ready for integration tests"
else
    echo "  ⚠ Docker not available (integration tests may be limited)"
fi
echo "  ✓ Project structure valid"
echo "  ✓ Dependencies resolved"
echo "  ✓ Tests compile successfully"

echo ""
print_status "You can now run the full test suite with:"
echo "  ./run-tests.sh                    # Full test suite"
echo "  ./run-tests.sh --skip-performance # Skip performance tests"
echo "  mvn test                          # Maven test command"

echo ""
print_success "Test environment is ready! 🧪"