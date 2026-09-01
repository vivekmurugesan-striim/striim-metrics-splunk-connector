#!/bin/bash

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Stopping Striim Splunk Connector...${NC}"

docker-compose down

echo -e "${GREEN}Services stopped!${NC}"
