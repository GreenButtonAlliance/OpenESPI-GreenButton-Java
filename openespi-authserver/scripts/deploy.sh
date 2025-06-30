#!/bin/bash

# OpenESPI Authorization Server Deployment Script
# Provides automated deployment for various environments

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DOCKER_DIR="$PROJECT_ROOT/docker"

# Default configuration
ENVIRONMENT="development"
PROFILE=""
BUILD_IMAGE=false
CLEAN_VOLUMES=false
VERBOSE=false
DRY_RUN=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Usage information
usage() {
    cat << EOF
OpenESPI Authorization Server Deployment Script

Usage: $0 [OPTIONS]

OPTIONS:
    -e, --environment ENV    Deployment environment (development|production|testing)
    -p, --profile PROFILE    Additional Docker Compose profiles (postgresql|monitoring|logging|nginx|redis)
    -b, --build             Build Docker image before deployment
    -c, --clean             Clean volumes before deployment
    -v, --verbose           Enable verbose output
    -d, --dry-run           Show commands that would be executed without running them
    -h, --help              Show this help message

ENVIRONMENTS:
    development    Local development environment with MySQL
    production     Production environment with external dependencies
    testing        Testing environment with minimal services

PROFILES:
    postgresql     Use PostgreSQL instead of MySQL
    monitoring     Enable Prometheus and Grafana
    logging        Enable ELK stack for log aggregation
    nginx          Enable NGINX reverse proxy
    redis          Enable Redis for session storage
    datacustodian  Include DataCustodian service for integration testing

EXAMPLES:
    # Basic development deployment
    $0 -e development

    # Production deployment with monitoring
    $0 -e production -p monitoring -b

    # Development with PostgreSQL and monitoring
    $0 -e development -p postgresql,monitoring

    # Clean deployment with all services
    $0 -e development -p postgresql,monitoring,logging,redis -c -b

    # Dry run to see what would be executed
    $0 -e production -p monitoring -d

EOF
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -e|--environment)
                ENVIRONMENT="$2"
                shift 2
                ;;
            -p|--profile)
                PROFILE="$2"
                shift 2
                ;;
            -b|--build)
                BUILD_IMAGE=true
                shift
                ;;
            -c|--clean)
                CLEAN_VOLUMES=true
                shift
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            -d|--dry-run)
                DRY_RUN=true
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                usage
                exit 1
                ;;
        esac
    done
}

# Execute command with dry-run support
execute_command() {
    local cmd="$1"
    local description="$2"
    
    if [[ $VERBOSE == true ]]; then
        log_info "Executing: $description"
        log_info "Command: $cmd"
    fi
    
    if [[ $DRY_RUN == true ]]; then
        echo "[DRY-RUN] $cmd"
    else
        if [[ $VERBOSE == true ]]; then
            eval "$cmd"
        else
            eval "$cmd" > /dev/null 2>&1
        fi
    fi
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if Docker is installed
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker and try again."
        exit 1
    fi
    
    # Check if Docker Compose is installed
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "Docker Compose is not installed. Please install Docker Compose and try again."
        exit 1
    fi
    
    # Check if Docker daemon is running
    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running. Please start Docker and try again."
        exit 1
    fi
    
    # Check if .env file exists for production
    if [[ $ENVIRONMENT == "production" && ! -f "$DOCKER_DIR/.env" ]]; then
        log_warning ".env file not found for production deployment."
        log_info "Copying .env.example to .env. Please customize it before deployment."
        cp "$DOCKER_DIR/.env.example" "$DOCKER_DIR/.env"
        log_warning "Please edit $DOCKER_DIR/.env with your production configuration."
        if [[ $DRY_RUN == false ]]; then
            read -p "Press Enter to continue after editing .env file..."
        fi
    fi
    
    log_success "Prerequisites check completed."
}

# Build Docker image
build_image() {
    if [[ $BUILD_IMAGE == true ]]; then
        log_info "Building Docker image..."
        execute_command "cd '$PROJECT_ROOT' && docker build -f docker/Dockerfile -t espi-authserver:latest ." "Building application image"
        log_success "Docker image built successfully."
    fi
}

# Clean volumes
clean_volumes() {
    if [[ $CLEAN_VOLUMES == true ]]; then
        log_warning "Cleaning Docker volumes..."
        
        case $ENVIRONMENT in
            development)
                execute_command "docker volume rm -f espi-authserver-logs espi-authserver-config espi-mysql-data espi-redis-data espi-prometheus-data espi-grafana-data 2>/dev/null || true" "Cleaning development volumes"
                ;;
            production)
                execute_command "docker volume rm -f espi-authserver-logs-prod espi-authserver-config-prod espi-redis-data-prod espi-prometheus-data-prod espi-grafana-data-prod 2>/dev/null || true" "Cleaning production volumes"
                ;;
            testing)
                execute_command "docker volume rm -f espi-authserver-logs-test espi-authserver-config-test espi-mysql-data-test 2>/dev/null || true" "Cleaning testing volumes"
                ;;
        esac
        
        log_success "Volumes cleaned."
    fi
}

# Deploy services
deploy_services() {
    log_info "Deploying OpenESPI Authorization Server ($ENVIRONMENT environment)..."
    
    cd "$DOCKER_DIR"
    
    # Prepare Docker Compose command
    local compose_cmd="docker-compose"
    local compose_files=""
    local profiles=""
    
    # Select compose file based on environment
    case $ENVIRONMENT in
        development)
            compose_files="-f docker-compose.yml"
            ;;
        production)
            compose_files="-f docker-compose.production.yml"
            ;;
        testing)
            compose_files="-f docker-compose.yml -f docker-compose.testing.yml"
            ;;
        *)
            log_error "Invalid environment: $ENVIRONMENT"
            exit 1
            ;;
    esac
    
    # Add profiles if specified
    if [[ -n $PROFILE ]]; then
        IFS=',' read -ra PROFILE_ARRAY <<< "$PROFILE"
        for p in "${PROFILE_ARRAY[@]}"; do
            profiles="$profiles --profile $p"
        done
    fi
    
    # Stop existing services
    execute_command "$compose_cmd $compose_files down" "Stopping existing services"
    
    # Pull latest images (except for local builds)
    if [[ $BUILD_IMAGE == false ]]; then
        execute_command "$compose_cmd $compose_files pull" "Pulling latest images"
    fi
    
    # Start services
    execute_command "$compose_cmd $compose_files $profiles up -d" "Starting services"
    
    log_success "Services deployed successfully."
}

# Wait for services to be ready
wait_for_services() {
    log_info "Waiting for services to be ready..."
    
    local max_attempts=30
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        if [[ $DRY_RUN == true ]]; then
            log_info "[DRY-RUN] Would check service health"
            break
        fi
        
        # Check if Authorization Server is healthy
        if curl -f -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
            log_success "Authorization Server is ready!"
            break
        fi
        
        log_info "Waiting for services... (attempt $attempt/$max_attempts)"
        sleep 10
        ((attempt++))
    done
    
    if [[ $attempt -gt $max_attempts && $DRY_RUN == false ]]; then
        log_error "Services did not become ready within expected time."
        log_info "Check service logs: docker-compose logs authserver"
        exit 1
    fi
}

# Display deployment information
show_deployment_info() {
    log_success "Deployment completed successfully!"
    echo
    log_info "Service Information:"
    echo "  Environment: $ENVIRONMENT"
    if [[ -n $PROFILE ]]; then
        echo "  Profiles: $PROFILE"
    fi
    echo
    log_info "Access URLs:"
    echo "  Authorization Server: http://localhost:8080"
    echo "  Management Endpoints: http://localhost:8081/actuator"
    echo "  Health Check: http://localhost:8081/actuator/health"
    
    if [[ $PROFILE == *"monitoring"* ]]; then
        echo "  Grafana: http://localhost:3000 (admin/admin)"
        echo "  Prometheus: http://localhost:9090"
    fi
    
    if [[ $PROFILE == *"nginx"* ]]; then
        echo "  NGINX Proxy: http://localhost"
    fi
    
    echo
    log_info "Useful Commands:"
    echo "  View logs: docker-compose logs -f authserver"
    echo "  Stop services: docker-compose down"
    echo "  Restart services: docker-compose restart"
    echo "  Check status: docker-compose ps"
    echo
    
    if [[ $ENVIRONMENT == "production" ]]; then
        log_warning "Production Checklist:"
        echo "  ✓ Verify SSL certificates are properly configured"
        echo "  ✓ Check that all passwords are changed from defaults"
        echo "  ✓ Ensure backup procedures are in place"
        echo "  ✓ Review security configuration"
        echo "  ✓ Test certificate authentication if enabled"
    fi
}

# Main execution function
main() {
    echo "OpenESPI Authorization Server Deployment Script"
    echo "=============================================="
    echo
    
    parse_args "$@"
    check_prerequisites
    build_image
    clean_volumes
    deploy_services
    wait_for_services
    show_deployment_info
}

# Run main function with all arguments
main "$@"