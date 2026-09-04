# Striim-Splunk Connector Architecture

## System Overview

The Striim-Splunk Connector is a Spring Boot application that bridges Striim and Splunk, enabling real-time monitoring of Striim applications through automated metrics collection and publishing.

## Component Architecture Diagram

### Visual Architecture (SVG)

![Architecture Diagram](docs/architecture-diagram.svg)

*The diagram above shows the complete flow from user interaction through data collection, processing, publishing, and visualization.*

### Architecture Layers Explained

**Layer 1: User Interaction**
- React frontend provides UI for configuration and monitoring
- Users configure Striim and Splunk credentials
- Manual trigger for immediate metrics collection
- View execution history and collection status

**Layer 2: Application Backend (Spring Boot)**
- REST API layer handling all client requests
- Service layer with business logic
- Multiple specialized services for different functions
- PostgreSQL database for persistent storage
- Two main pipelines: Striim data collection and Splunk publishing

**Layer 3: External Systems & Storage**
- Striim Server: Source of metrics data via mon commands
- Splunk HEC: Destination for metrics publishing
- Splunk Index: Stores and organizes metrics
- Splunk Dashboard: Visualizes collected data

## Data Flow Sequences

### 1. Configuration & Initialization Flow

```
User                Frontend              Backend              Database
  │                   │                    │                      │
  │─ Configure ─────→│                    │                      │
  │  Striim/Splunk   │─ POST /v1/config ─→│                      │
  │                   │                    │─ Encrypt & Save ────→│
  │                   │                    │─ Update Interval ────→ Reschedule Task
  │                   │← Success Response ←│                      │
  │← Display Config ←│                    │                      │
```

### 2. Metrics Collection Flow (Manual Trigger)

```
User              Frontend              Backend            Striim           Splunk
  │                 │                    │                  │                │
  │─ Click Trigger→│                    │                  │                │
  │                 │─ POST /v1/collect→│                  │                │
  │                 │                    │─ Authenticate ──→│                │
  │                 │                    │← Token Back ─────│                │
  │                 │                    │─ Execute Mon ───→│                │
  │                 │                    │← JSON Response ──│                │
  │                 │                    │- Parse & Count   │                │
  │                 │                    │- Add Timestamp   │                │
  │                 │                    │─ Publish HEC ────────────────────→│
  │                 │                    │← HEC Ack ────────────────────────│
  │                 │← Status Response ←│                  │                │
  │← Show Success ←│                    │                  │                │
```

### 3. Scheduled Metrics Collection Flow

```
Scheduler        MetricsService        StriimApiClient     SplunkHecClient
    │                 │                      │                    │
    │─ Trigger Time ─→│                      │                    │
    │                 │─ Load Config ────→ DB                    │
    │                 │─ Check Interval ────→                    │
    │                 │─ Execute Commands ──→│                    │
    │                 │                      │─ Auth Striim      │
    │                 │                      │─ Run Mon Commands │
    │                 │← Metrics JSON ──────│                    │
    │                 │─ Parse & Store ────→ DB                 │
    │                 │─ Publish to Splunk ──────────────────→│
    │                 │                                          │─ Index Event
    │                 │← Acknowledgment ────────────────────←│
```

### 4. Dashboard Visualization Flow

```
Splunk Index         Splunk Dashboard         Browser
    │                      │                    │
    │← Query Events ───────│                    │
    │─ Aggregate Data ────→│                    │
    │                      │─ Render Panels ──→ │
    │                      │                    │─ Display Charts
    │                      │                    │─ Show Tables
    │                      │                    │─ Update Timeline
    │                      │                    │
    │                      │← Refresh on Demand├─ User Views
    │                      │                    │
```

## Key Components Details

### Frontend (React)
- **Port:** 3000
- **Endpoints Called:**
  - `GET/POST /v1/config` - Configuration management
  - `POST /v1/collect/trigger` - Manual metrics collection
  - `GET /v1/collect/commands` - Available commands list
  - `GET /v1/history` - Execution history

### Backend (Spring Boot)
- **Port:** 8080 (REST API)
- **Services:**
  - **ConfigService:** Manages credentials, encryption, database persistence
  - **MetricsCollectionService:** Orchestrates collection, parsing, and publishing
  - **StriimApiClient:** Handles Striim authentication and mon command execution
  - **SplunkHecClient:** Publishes metrics to Splunk HTTP Event Collector
  - **SchedulingConfig:** Dynamic task scheduling with configurable intervals

### Database (PostgreSQL)
- **Tables:**
  - `system_config` - Striim URL, credentials, Splunk settings, collection interval
  - `execution_history` - Execution ID, status, metrics count, timestamps

### Striim Server
- **Endpoints:**
  - `POST /security/authenticate` - Form-encoded authentication (username/password)
  - `POST /api/v2/tungsten` - Mon command execution
- **Authentication:** Token-based (55-minute expiry, automatic re-authentication)

### Splunk
- **HTTP Event Collector:** Port 8088
  - Token-based authentication
  - Accepts JSON events
  - Sourcetype: `_json`
  - Index: `striim_app_mon`

### Splunk Dashboard
- **Features:**
  - 9 interactive panels
  - Real-time metrics visualization
  - Application status breakdown
  - Timeline of collection events
  - 24-hour default time range

## Data Model

### Metrics Structure

```json
{
  "metric_0": {
    "command": "mon;",
    "timestamp": 1788460521494,
    "totalApplications": 38,
    "runningApplications": 1,
    "stoppedApplications": 9,
    "createdApplications": 24,
    "haltApplications": 1,
    "terminatedApplications": 1,
    "completedApplications": 1,
    "notEnoughServersApplications": 1,
    "applications": [
      {
        "name": "admin.ApplicationName",
        "status": "RUNNING",
        "rate": "100",
        "numServers": "2",
        "cpuRate": "45%"
      }
    ]
  }
}
```

## Security Architecture

### Credential Management
- Passwords and tokens encrypted at rest using AES encryption
- Encryption keys stored in application configuration
- Tokens automatically refreshed (55-minute expiry)

### API Security
- Spring Security configured for REST endpoints
- CORS handling for frontend requests
- Context path `/api` with version-based routing (`/v1`)

### Network Communication
- Striim: Form-encoded authentication, Token header for subsequent requests
- Splunk HEC: Bearer token in Authorization header
- X-Splunk-Request-Channel header for indexer acknowledgment

## Deployment Architecture

### Docker Containers
- **Frontend:** Node.js + React (port 3000)
- **Backend:** Java 17 + Spring Boot (ports 8080, 9080)
- **Database:** PostgreSQL 15 (port 5432)
- **Splunk:** Splunk Enterprise (port 8000, 8088)
- **Striim:** External (configured URL)

### Container Networking
- Docker Compose network enables service-to-service communication
- Environment variables for connection strings
- Health checks for dependency management

## Configuration Flow

```
User Input (UI)
    ↓
ConfigPanel Component
    ↓
POST /v1/config
    ↓
ConfigController
    ↓
ConfigService
    ↓
SystemConfig Entity
    ↓
PostgreSQL (encrypted)
    ↓
ConfigController calls MetricsCollectionService.updateCollectionInterval()
    ↓
TaskScheduler reschedules collection task
    ↓
Next collection uses new interval
```

## Performance Characteristics

- **Collection Interval:** 60 seconds (default, configurable 10+ seconds)
- **Token Expiry:** 55 minutes (automatic refresh on next collection)
- **Dashboard Query Time Range:** 24 hours (customizable)
- **Metrics Storage:** All events indexed in Splunk
- **Scheduled Task:** Single thread, non-blocking execution
- **Splunk HEC:** Asynchronous publishing with channel-based acknowledgment

## Error Handling & Recovery

1. **Authentication Failures:** Automatic retry on token expiry
2. **Network Failures:** Logged and tracked in execution_history
3. **Parsing Errors:** Fallback to plain-text parsing if JSON fails
4. **Splunk Unavailable:** Collection continues, publishing queued for retry
5. **Configuration Missing:** Scheduled collection skipped until configured

## Monitoring & Observability

- **Backend Logs:** DEBUG level for parsing details, INFO for major events
- **Execution History:** Tracks all collection runs with status and error details
- **Dashboard Queries:** Can view raw JSON events for debugging
- **Splunk Index:** All events searchable with full audit trail
