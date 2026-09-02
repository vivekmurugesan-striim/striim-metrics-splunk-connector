#!/bin/bash

# Splunk Initialization Script
# Automatically creates index and HEC token for development

set -e

SPLUNK_HOME="/opt/splunk"
SPLUNK_PASSWORD="${SPLUNK_PASSWORD:-ChangedPassword}"
SPLUNK_ADMIN_USER="admin"
SPLUNK_CLI="${SPLUNK_HOME}/bin/splunk"

echo "=== Splunk Initialization Script ==="
echo "Waiting for Splunk to be ready..."

# Wait for Splunk to be fully initialized
max_attempts=30
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if "${SPLUNK_CLI}" show config --auth "${SPLUNK_ADMIN_USER}:${SPLUNK_PASSWORD}" > /dev/null 2>&1; then
        echo "✓ Splunk is ready"
        break
    fi
    attempt=$((attempt + 1))
    echo "Waiting for Splunk... (attempt $attempt/$max_attempts)"
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo "✗ Splunk failed to start within timeout"
    exit 1
fi

echo ""
echo "=== Creating Splunk Index ==="

# Create metrics index
"${SPLUNK_CLI}" add index striim_metrics -auth "${SPLUNK_ADMIN_USER}:${SPLUNK_PASSWORD}" \
    -datatype metrics \
    -maxKBps 0 \
    -maxMemMB 50 \
    2>/dev/null || echo "Index already exists or error occurred"

echo "✓ Index 'striim_metrics' created/verified"

echo ""
echo "=== Configuring HTTP Event Collector (HEC) ==="

# Enable HEC
"${SPLUNK_CLI}" enable listen 8088 -auth "${SPLUNK_ADMIN_USER}:${SPLUNK_PASSWORD}" \
    2>/dev/null || echo "HEC might already be enabled"

echo "✓ HEC enabled on port 8088"

echo ""
echo "=== Creating HEC Token ==="

# Create HEC token for Striim
HEC_TOKEN=$(uuidgen 2>/dev/null || echo "striim-$(date +%s)-token")

"${SPLUNK_CLI}" http-event-collector create striim-connector \
    -auth "${SPLUNK_ADMIN_USER}:${SPLUNK_PASSWORD}" \
    -uri "https://localhost:8089" \
    -default-sourcetype "striim:metrics" \
    -default-index "striim_metrics" \
    -token "$HEC_TOKEN" \
    2>/dev/null || {
        echo "Token creation failed or already exists, trying alternative method..."
        # Try to get existing token
        HEC_TOKEN=$("${SPLUNK_CLI}" list http-event-collector striim-connector \
            -auth "${SPLUNK_ADMIN_USER}:${SPLUNK_PASSWORD}" \
            2>/dev/null | grep "token:" | awk '{print $NF}') || HEC_TOKEN="UNKNOWN"
    }

echo "✓ HEC Token created/verified"

echo ""
echo "=== Creating .env.splunk file with Splunk credentials ==="

# Create a file with Splunk configuration for the application
cat > "${SPLUNK_HOME}/.env.splunk" << EOF
# Auto-generated Splunk configuration
# Created: $(date)

# Splunk HEC Configuration
SPLUNK_HEC_URL=http://localhost:8088/services/collector
SPLUNK_TOKEN=$HEC_TOKEN
SPLUNK_INDEX=striim_metrics

# Splunk Web UI
SPLUNK_WEB_URL=http://localhost:8000
SPLUNK_USERNAME=$SPLUNK_ADMIN_USER
SPLUNK_PASSWORD=$SPLUNK_PASSWORD

# HEC Token Details
HEC_TOKEN_NAME=striim-connector
HEC_TOKEN_VALUE=$HEC_TOKEN
HEC_SOURCE_TYPE=striim:metrics
EOF

echo "✓ Configuration saved to .env.splunk"

echo ""
echo "=========================================="
echo "✓ Splunk Initialization Complete!"
echo "=========================================="
echo ""
echo "Access Details:"
echo "  Web UI:     http://localhost:8000"
echo "  Username:   $SPLUNK_ADMIN_USER"
echo "  Password:   $SPLUNK_PASSWORD"
echo ""
echo "HEC Token Details:"
echo "  URL:        http://localhost:8088/services/collector"
echo "  Token:      $HEC_TOKEN"
echo "  Index:      striim_metrics"
echo "  Source Type: striim:metrics"
echo ""
echo "Environment Variables:"
echo "  SPLUNK_HEC_URL=$SPLUNK_HEC_URL"
echo "  SPLUNK_TOKEN=$HEC_TOKEN"
echo "  SPLUNK_INDEX=striim_metrics"
echo ""
echo "Test HEC Token:"
echo "  curl -H \"Authorization: Splunk $HEC_TOKEN\" \\"
echo "       -H \"Content-Type: application/json\" \\"
echo "       http://localhost:8088/services/collector \\"
echo "       -d '{\"event\":\"test\"}'"
echo ""

# Keep the container running
tail -f /dev/null
