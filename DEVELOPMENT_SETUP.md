# Development Environment Setup

This guide covers running the Striim Splunk Connector with automatically configured Splunk for development.

## Quick Start with Auto-Splunk Setup (Recommended for Development)

The easiest way to set up your development environment is to use `docker-compose.dev.yml`, which automatically:
- Spins up Splunk Docker container
- Creates the `striim_metrics` index
- Generates and configures HEC token
- Initializes all necessary settings

### Prerequisites

- Docker and Docker Compose installed
- 6GB+ free disk space (for containers)
- Ports available: 3000, 5432, 8000, 8088, 8080

### Start Everything

```bash
# Clone the repository
git clone https://github.com/vivekmurugesan-striim/striim-metrics-splunk-connector.git
cd striim-metrics-splunk-connector

# Copy environment file
cp .env.example .env

# Start all services with auto-configured Splunk
docker-compose -f docker-compose.dev.yml up -d

# Wait for services to initialize (about 2-3 minutes)
# Watch the logs
docker-compose -f docker-compose.dev.yml logs -f

# Once you see the HEC token details printed, you're ready!
```

### Access the Application

Once all services are running:

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **Splunk Web UI**: http://localhost:8000
  - Username: `admin`
  - Password: `ChangedPassword` (or `$SPLUNK_PASSWORD` from `.env`)

### Get HEC Token from Running Container

The HEC token is auto-generated and printed to the console during initialization. To retrieve it:

```bash
# View Splunk initialization output
docker-compose -f docker-compose.dev.yml logs splunk | grep -A 20 "HEC Token"

# Or check the generated config file
docker exec striim-connector-splunk cat /opt/splunk/.env.splunk
```

### Configure Striim Connector

1. Access http://localhost:3000
2. Go to **Configuration** tab
3. Enter your Striim server details
4. For Splunk settings, use:
   - **Splunk HEC URL**: `http://localhost:8088/services/collector`
   - **Splunk HEC Token**: (from the initialization output above)
   - **Splunk Index**: `striim_metrics`
5. Save configuration
6. Click **Trigger Metrics Collection** to test

## Production Setup

For production, use the standard `docker-compose.yml` and connect to your production Splunk instance:

```bash
# Use regular docker-compose.yml (without the .dev suffix)
docker-compose up -d

# Set environment variables pointing to production Splunk
export SPLUNK_HEC_URL=https://your-production-splunk.com:8088/services/collector
export SPLUNK_TOKEN=your-production-hec-token
export SPLUNK_INDEX=striim_metrics
```

See [SPLUNK_SETUP.md](SPLUNK_SETUP.md) for detailed production Splunk configuration.

## What Gets Auto-Configured

The `splunk-init.sh` script automatically:

1. **Creates Index**: `striim_metrics` configured for metrics data
2. **Enables HEC**: HTTP Event Collector on port 8088
3. **Creates Token**: Named `striim-connector` with:
   - Default Index: `striim_metrics`
   - Default Source Type: `striim:metrics`
   - Full permissions to write to `striim_metrics`
4. **Prints Configuration**: Outputs all credentials and settings to console
5. **Saves to File**: Creates `/opt/splunk/.env.splunk` with all settings

## Common Development Tasks

### View Splunk Logs

```bash
docker-compose -f docker-compose.dev.yml logs splunk
```

### Access Splunk CLI

```bash
docker exec striim-connector-splunk /opt/splunk/bin/splunk list http-event-collector -auth admin:ChangedPassword
```

### Restart Splunk Service

```bash
docker-compose -f docker-compose.dev.yml restart splunk
```

### View Collected Metrics in Splunk

1. Access Splunk: http://localhost:8000
2. Go to **Search & Reporting**
3. Search:
   ```spl
   index=striim_metrics sourcetype=striim:metrics
   | tail 20
   ```

### Reset Everything

```bash
# Stop all services
docker-compose -f docker-compose.dev.yml down

# Remove volumes (careful - deletes data)
docker volume rm striim-connector-splunk striim-connector-db

# Start fresh
docker-compose -f docker-compose.dev.yml up -d
```

## Switching to Production Splunk

Once you have a production Splunk instance:

1. **Stop dev Splunk**:
   ```bash
   docker-compose -f docker-compose.dev.yml down
   ```

2. **Update `.env` with production Splunk credentials**:
   ```bash
   SPLUNK_HEC_URL=https://your-prod-splunk.com:8088/services/collector
   SPLUNK_TOKEN=your-prod-hec-token
   SPLUNK_INDEX=striim_metrics
   ```

3. **Use standard docker-compose.yml**:
   ```bash
   docker-compose up -d
   ```

## Troubleshooting

### Splunk container not starting

```bash
# Check logs
docker-compose -f docker-compose.dev.yml logs splunk

# Common issues:
# - Not enough disk space
# - Port 8000 or 8088 already in use
# - Docker daemon not running
```

### HEC Token not generated

```bash
# Check if Splunk is healthy
curl http://localhost:8000

# View initialization logs
docker logs striim-connector-splunk

# Manually check Splunk
docker exec striim-connector-splunk \
  /opt/splunk/bin/splunk list http-event-collector \
  -auth admin:ChangedPassword
```

### Cannot connect to HEC

```bash
# Verify HEC is listening
docker exec striim-connector-splunk \
  /opt/splunk/bin/splunk list listen \
  -auth admin:ChangedPassword

# Test connectivity
curl -H "Authorization: Splunk YOUR-TOKEN" \
  http://localhost:8088/services/collector \
  -d '{"event":"test"}'
```

### Metrics not appearing in Splunk

1. Verify collection is running:
   - Check http://localhost:3000 dashboard for recent executions
   
2. Verify HEC token in logs:
   ```spl
   index=_internal source=*http_event_collector.log*
   | stats count by status
   ```

3. Check index exists:
   ```spl
   | rest /services/data/indexes | search title=striim_metrics
   ```

## Next Steps

- See [README.md](README.md) for feature overview
- See [DEVELOPMENT.md](DEVELOPMENT.md) for backend/frontend development
- See [SPLUNK_SETUP.md](SPLUNK_SETUP.md) for production Splunk setup
- See [METRICS.md](METRICS.md) for metrics specifications

---

**Happy developing!** 🚀

If you encounter any issues, please:
1. Check the troubleshooting section above
2. Review container logs: `docker-compose -f docker-compose.dev.yml logs`
3. Ensure all prerequisites are installed
4. Check available disk space and ports
