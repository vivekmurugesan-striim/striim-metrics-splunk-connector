#!/bin/bash

set -e

echo "Building Striim Splunk Connector..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed${NC}"
    exit 1
fi

# Check if docker-compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}Error: Docker Compose is not installed${NC}"
    exit 1
fi

# Build the images
echo -e "${YELLOW}Building Docker images...${NC}"
docker-compose build

echo -e "${GREEN}Build completed successfully!${NC}"
echo ""
echo "To start the application, run:"
echo "  docker-compose up -d"
echo ""
echo "Then access:"
echo "  Frontend: http://localhost:3000"
echo "  Backend API: http://localhost:8080/api"
echo "  PostgreSQL: localhost:5432"
