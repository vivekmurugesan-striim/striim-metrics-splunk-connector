# Striim Splunk Connector

A utility to connect Striim using REST API, collect monitoring metrics, and publish them to Splunk for comprehensive observability.

## Features

- **Striim Integration**: Connect to Striim servers via REST API and execute monitoring commands
- **Dynamic Authentication**: Automatically authenticate with Striim using username/password and manage tokens
- **Metrics Collection**: Extract system metrics (CPU, Memory, Disk) and Striim-specific metrics
- **Splunk Publishing**: Publish collected metrics to Splunk HEC (HTTP Event Collector)
- **Web Dashboard**: React-based UI for configuration, monitoring, and management
- **Scheduled Collection**: Automatic metric collection at configurable intervals
- **Manual Triggers**: Trigger metrics collection on-demand through the UI
- **Execution History**: Track and view historical execution runs
- **PostgreSQL Database**: Store configuration, schedules, and execution history

## Technology Stack

- **Backend**: Java 17, Spring Boot 3.1
- **Frontend**: React 18, JavaScript
- **Database**: PostgreSQL 15
- **Authentication**: Username/Password with dynamic token generation
- **Deployment**: Docker & Docker Compose

## Project Structure

```
striim-splunk-connector/
├── src/main/java/com/striim/
│   ├── controller/              # REST API controllers
│   ├── entity/                  # Database entities
│   ├── repository/              # Data access layer
│   ├── service/                 # Business logic
│   ├── dto/                     # Data transfer objects
│   └── util/                    # Utility classes
├── src/main/resources/
│   ├── application.yml          # Spring Boot configuration
│   └── db/migration/            # Database migrations
├── frontend/
│   ├── src/
│   │   ├── api/                 # API client service
│   │   ├── components/          # React components
│   │   ├── App.js               # Main App component
│   │   └── App.css              # Styling
│   ├── public/                  # Static assets
│   ├── Dockerfile               # Frontend Docker image
│   └── nginx.conf               # Nginx configuration
├── pom.xml                      # Maven configuration
├── Dockerfile                   # Backend Docker image
├── docker-compose.yml           # Docker Compose orchestration
└── README.md                    # This file
```

## Getting Started

### Prerequisites

- Docker and Docker Compose installed
- Java 17+ (for local development)
- Node.js 18+ (for frontend development)
- PostgreSQL 15 (for local development)

### Quick Start with Docker Compose

1. **Clone and setup**:
```bash
cd striim-splunk-connector
cp .env.example .env
```

2. **Start all services**:
```bash
docker-compose up -d
```

3. **Access the application**:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- PostgreSQL: localhost:5432

4. **Configure Striim and Splunk**:
   - Navigate to the "Configuration" tab in the UI
   - Enter your Striim URL, username, and password
   - Token will be automatically generated via the `/authenticate` endpoint
   - Enter your Splunk HEC URL and token
   - Set the target Splunk index and collection interval
   - Click "Save Configuration"

### Local Development

#### Backend Setup

```bash
# Build the backend
mvn clean package

# Run the application
java -jar target/striim-splunk-connector-1.0.0.jar
```

The backend will start on http://localhost:8080

#### Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start
```

The frontend will start on http://localhost:3000

### Environment Variables

Create a `.env` file based on `.env.example`:

```env
DB_USERNAME=postgres
DB_PASSWORD=your-secure-password
JWT_SECRET=your-secret-key
COLLECTION_INTERVAL=60
STRIIM_URL=http://localhost:9080
STRIIM_USER=admin
STRIIM_PASSWORD=your-password
```

## API Endpoints

### Configuration Management

**POST /api/v1/config** - Save or update configuration
```json
{
  "striimUrl": "http://localhost:9080",
  "striimUser": "admin",
  "striimPassword": "your-password",
  "splunkHecUrl": "https://splunk.company.com:8088/services/collector",
  "splunkToken": "your-hec-token",
  "splunkIndex": "striim_metrics",
  "collectionIntervalSeconds": 60
}
```

**GET /api/v1/config** - Get current configuration

### Metrics Collection

**POST /api/v1/collect/trigger** - Manually trigger collection
```json
{
  "targetCommands": ["mon system", "mon apps"]
}
```

**GET /api/v1/collect/status/{executionId}** - Get execution status

### History

**GET /api/v1/history** - Fetch execution history (last 100 runs)

## Database Schema

### system_config Table
- `id` - Configuration identifier
- `striim_url` - Striim API endpoint
- `striim_user` - Striim username
- `striim_password_enc` - Encrypted Striim password
- `splunk_hec_url` - Splunk HEC endpoint
- `splunk_token_enc` - Encrypted Splunk token
- `splunk_index` - Target Splunk index
- `interval_seconds` - Collection interval
- `updated_at` - Last update timestamp

### execution_history Table
- `execution_id` - Unique execution identifier
- `trigger_type` - SCHEDULED or MANUAL
- `status` - RUNNING, COMPLETED, or FAILED
- `metrics_collected_count` - Number of metrics collected
- `error_message` - Error details if failed
- `start_time` - Execution start timestamp
- `end_time` - Execution completion timestamp
- `published_to_splunk` - Whether metrics were published

## Striim Authentication

The connector uses Striim's `/api/v2/authenticate` endpoint to obtain access tokens dynamically:

1. **Configuration**: Store Striim username and password (encrypted at rest)
2. **Authentication**: Call `/authenticate` to get a token valid for 30 minutes
3. **Token Caching**: Tokens are cached and reused until expiration
4. **Auto-Refresh**: New tokens are automatically generated when needed
5. **Command Execution**: Use obtained token to execute `mon` commands via `/api/v2/tungsten`

Reference: https://github.com/striim/rest-api-samples/tree/master/v2

## Metrics Collected

### System & Infrastructure
- CPU Utilization
- JVM Memory Usage
- Disk Space / Storage
- Network Egress/Ingress

### Application & Pipeline Health
- Pipeline Status (Running/Down)
- Node Availability
- JVM Restarts
- Application Health Status

### Throughput & Data Flow
- Event Processing Throughput (records/sec)
- Source Read Count
- Target Write Count
- Table Synchronization Counts

### Lag & Latency
- End-to-End Latency (source to sink)
- CDC Replication Lag
- Checkpoint Delay
- LEE Metrics (pipeline lag)

### Queue & Backlog
- Queue Depth
- Backpressure State
- Message Backlog

### Integration & Target Sinks
- Kafka Publish/Consume Errors
- BigQuery Write Latency
- OJet (Oracle) SCN transaction backlog

### Security & Logging
- Server Log Errors/Warnings
- Smart Alerts
- Login Failures
- Pipeline Configuration Modifications

## UI Components

### Dashboard
- System health metrics display
- Recent job status cards
- Manual trigger button for metrics collection
- Recent execution history table

### Configuration Panel
- Striim endpoint, username, and password settings
- Splunk HEC endpoint and authentication settings
- Collection interval configuration
- Index name settings
- Form validation and error handling

### Execution History Table
- Paginated view of historical executions
- Status filtering and sorting
- Duration calculation
- Splunk publication status
- Auto-refresh capability

## Security Considerations

1. **Password Encryption**: All sensitive credentials (Striim password and Splunk token) are encrypted at rest using Base64
2. **Environment Variables**: Use environment variables for initial configuration setup
3. **Token Management**: Striim tokens are cached and auto-refreshed, never stored permanently
4. **JWT Authentication**: API endpoints can be secured with JWT tokens
5. **HTTPS**: Use HTTPS in production environments
6. **Network Isolation**: Run services on private networks with proper access controls

## Troubleshooting

### Authentication Errors

**"Failed to authenticate with Striim"**:
- Verify username and password are correct
- Ensure Striim server is accessible at the configured URL
- Check Striim logs for authentication failures
- Verify user has appropriate permissions in Striim

### Database Connection Issues
```bash
# Check PostgreSQL connectivity
docker-compose logs postgres

# Verify database is running
docker-compose ps
```

### Metrics Collection Issues
```bash
# Check backend logs
docker-compose logs backend

# Verify configuration
curl http://localhost:8080/api/v1/config
```

### Frontend Connection Issues
```bash
# Check if backend is accessible
curl http://localhost:8080/api/health

# Check CORS headers
docker-compose exec frontend nginx -T
```

## Metrics Export Format

Metrics are published to Splunk in the following JSON format:

```json
{
  "event": {
    "cpu_utilization": 45.5,
    "memory_usage": 72.3,
    "disk_space": 81.2,
    "pipeline_status": "RUNNING",
    "throughput": 1250,
    "latency_ms": 3.5
  },
  "sourcetype": "striim:metrics",
  "index": "striim_metrics",
  "time": 1693513800
}
```

## Performance Tuning

1. **Collection Interval**: Adjust in configuration panel based on your monitoring needs (default: 60 seconds)
2. **Database Connection Pool**: Configure in `application.yml` for backend
3. **Memory Settings**: Adjust JVM heap size in Dockerfile: `-Xmx512m -Xms256m`
4. **Token Caching**: Tokens are cached for 30 minutes to reduce authentication overhead
5. **Splunk Batch Size**: Modify publishing logic in `SplunkHecClient` for bulk operations

## Support and Contributing

For issues, feature requests, or contributions, please refer to the project documentation or contact the development team.

## License

This project is licensed under your organization's license terms.
