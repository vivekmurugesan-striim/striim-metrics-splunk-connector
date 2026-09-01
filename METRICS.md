# Metrics Documentation

Comprehensive documentation of metrics collected and published to Splunk.

## Metrics Categories

### 1. System & Infrastructure Metrics

System-level performance metrics from the Striim server.

| Metric | Unit | Threshold | Command | Description |
|--------|------|-----------|---------|-------------|
| CPU Utilization | % | > 80% | `mon system` | CPU usage percentage |
| JVM Memory Usage | % | > 85% | `mon system` | JVM heap memory usage |
| Disk Space / Storage | % | > 80% | `mon system` | Disk space consumption |
| Network Egress | bytes/sec | N/A | `mon system` | Outbound network traffic |
| Network Ingress | bytes/sec | N/A | `mon system` | Inbound network traffic |

**Source**: Striim Server (JMX / OS Level)

### 2. Application & Pipeline Health Metrics

Application-level health and status indicators.

| Metric | Status | Alert Threshold | Command | Description |
|--------|--------|-----------------|---------|-------------|
| Pipeline Status | Running/Down | Any Down | `mon apps` | Current pipeline state |
| Node Availability | % | < 99.95% | `mon apps` | Percentage of available nodes |
| JVM Restarts | count | > 3 per hour | `mon apps` | Number of JVM restarts |
| App Health Status | Healthy/Unhealthy | Unhealthy | `mon apps` | Overall application health |

**Source**: StriimWatcher / JMX

### 3. Throughput & Data Flow Metrics

Metrics related to data processing rates and flow.

| Metric | Unit | Alert Threshold | Command | Description |
|--------|------|-----------------|---------|-------------|
| Event Processing Throughput | records/sec | > 25% deviation | `mon apps` | Events processed per second |
| Source Read Count | count | Mismatch > 1% | `mon apps` | Records read from source |
| Target Write Count | count | Mismatch > 1% | `mon apps` | Records written to target |
| Table Sync Counts | count | Mismatch > 1% | `mon apps` | Table synchronization counter |

**Alert Condition**: Source-to-Target Record Count Mismatch
**Source**: StriimWatcher

### 4. Lag & Latency Metrics

Metrics measuring delays and latency in the pipeline.

| Metric | Unit | Alert Threshold | Command | Description |
|--------|------|-----------------|---------|-------------|
| End-to-End Latency | ms | > 10,000ms | `mon apps` | Source to sink latency |
| CDC Replication Lag | minutes | > 5 min | `mon apps` | Change Data Capture lag |
| Checkpoint Delay | minutes | > 5 min | `mon apps` | Checkpoint processing delay |
| LEE Metrics (Pipeline Lag) | ms | > 10,000ms | `mon apps` | Logical Event Execution lag |

**Source**: StriimWatcher (LEE / Checkpoint History)

### 5. Queue & Backlog Metrics

Queue and backpressure indicators.

| Metric | Unit | Alert Threshold | Command | Description |
|--------|------|-----------------|---------|-------------|
| Queue Depth | count | Growing | `mon apps` | Current queue depth |
| Backpressure State | state | Active/Inactive | `mon apps` | Backpressure condition state |
| Message Backlog | count | Growing | `mon apps` | Accumulated message backlog |

**Alert Condition**: Growing message backlog / sustained backpressure
**Source**: StriimWatcher

### 6. Integration & Target Sinks Metrics

Metrics from target integration points.

| Metric | Type | Alert Threshold | Command | Description |
|--------|------|-----------------|---------|-------------|
| Kafka Publish Errors | count | Any sustained error | `mon apps` | Kafka publish failures |
| Kafka Consume Errors | count | Any sustained error | `mon apps` | Kafka consume failures |
| BigQuery Write Latency | ms | > 30,000ms | `mon apps` | BigQuery write response time |
| OJet (Oracle) SCN Backlog | count | Growing | `mon apps` | Oracle transaction backlog |

**Source**: Target Sinks (BigQueryWriter / KafkaWriter)

### 7. Security & Operational Logging Metrics

Security and operational event metrics.

| Metric | Type | Alert Threshold | Command | Description |
|--------|------|-----------------|---------|-------------|
| Server Log Errors | count | Sustained P1 errors | `mon system` | Server error count |
| Server Log Warnings | count | High frequency | `mon system` | Server warning count |
| Smart Alerts | count | Critical events | `mon system` | Striim Smart Alerts |
| Login Failures | count | Multiple failures | `mon system` | Failed login attempts |
| Configuration Modifications | event | Any modification | `mon apps` | Config change events |

**Source**: Striim Server Log (Smart Alerts)

## Metric JSON Schema

### Event Structure

Metrics are published to Splunk in the following format:

```json
{
  "event": {
    "timestamp": "2026-09-01T21:05:00Z",
    "striim_instance": "prod-striim-01",
    "metrics": {
      "system": {
        "cpu_utilization_percent": 45.5,
        "jvm_memory_usage_percent": 72.3,
        "disk_space_percent": 81.2,
        "network_egress_bytes_per_sec": 1024000,
        "network_ingress_bytes_per_sec": 2048000
      },
      "application": {
        "pipeline_status": "RUNNING",
        "pipeline_name": "prod-pipeline",
        "node_availability_percent": 99.98,
        "jvm_restarts_per_hour": 0,
        "app_health_status": "HEALTHY"
      },
      "throughput": {
        "event_processing_throughput_records_per_sec": 1250,
        "source_read_count": 5000000,
        "target_write_count": 5000000,
        "table_sync_count": 500
      },
      "latency": {
        "end_to_end_latency_ms": 3500,
        "cdc_replication_lag_minutes": 0.5,
        "checkpoint_delay_minutes": 0.1,
        "lee_metrics_ms": 2500
      },
      "queue": {
        "queue_depth": 150,
        "backpressure_state": "INACTIVE",
        "message_backlog": 0
      },
      "integration": {
        "kafka_publish_errors": 0,
        "kafka_consume_errors": 0,
        "bigquery_write_latency_ms": 250,
        "oracle_scn_backlog": 0
      },
      "security": {
        "server_log_errors_count": 2,
        "server_log_warnings_count": 5,
        "smart_alerts_count": 0,
        "login_failures_count": 0
      }
    }
  },
  "sourcetype": "striim:metrics",
  "index": "striim_metrics",
  "time": 1693513500
}
```

## Metric Collection Commands

### mon system
Collects system-level metrics from the Striim server.

**Returns**:
- CPU utilization
- JVM memory usage
- Disk space information
- Network statistics
- System uptime
- Server log information

### mon apps
Collects application and pipeline metrics.

**Returns**:
- Pipeline status
- Node availability
- Application health
- Throughput statistics
- Latency metrics
- Queue information
- Integration sink metrics

## Alert Thresholds

### Critical Alerts (Page On-Call)

- Any Pipeline Down
- JVM Memory > 85%
- CPU > 80%
- Storage Consumption > 80%
- E2E Latency > 10 sec
- CDC Replication Lag > 5 min
- Any sustained Kafka errors
- BigQuery write latency > 30 sec
- Critical security events

### Warning Alerts

- Node Availability < 99.95%
- > 3 JVM Restarts per hour
- > 25% throughput deviation
- Source-to-Target Record Count Mismatch
- Checkpoint Delay > 5 min
- Growing message backlog
- Multiple login failures

## Splunk Search Examples

### Real-time Pipeline Health

```spl
sourcetype="striim:metrics" 
| stats latest(metrics.application.pipeline_status) as pipeline_status, 
         latest(metrics.application.node_availability_percent) as node_availability,
         latest(metrics.application.app_health_status) as health_status
         by striim_instance
| where pipeline_status!="RUNNING" OR node_availability < 99.95
```

### System Resource Monitoring

```spl
sourcetype="striim:metrics"
| timechart avg(metrics.system.cpu_utilization_percent) as cpu_avg,
            avg(metrics.system.jvm_memory_usage_percent) as memory_avg,
            avg(metrics.system.disk_space_percent) as disk_avg
            by striim_instance
| where cpu_avg > 80 OR memory_avg > 85 OR disk_avg > 80
```

### Throughput Analysis

```spl
sourcetype="striim:metrics"
| stats latest(metrics.throughput.event_processing_throughput_records_per_sec) as throughput,
         latest(metrics.throughput.source_read_count) as read_count,
         latest(metrics.throughput.target_write_count) as write_count
         by striim_instance
| eval record_match_percent = round(write_count/read_count*100, 2)
```

### Latency Tracking

```spl
sourcetype="striim:metrics"
| timechart avg(metrics.latency.end_to_end_latency_ms) as e2e_latency,
            avg(metrics.latency.cdc_replication_lag_minutes) as replication_lag,
            avg(metrics.latency.lee_metrics_ms) as pipeline_lag
            by striim_instance
```

## Metric Collection Frequency

- **Default Interval**: 60 seconds
- **Configurable**: Yes, via UI or API
- **Minimum**: 10 seconds (recommended 30s+)
- **Maximum**: No limit (consider performance impact)

## Performance Impact

- **Network Usage**: ~5KB per collection (depending on metrics)
- **CPU Impact**: Minimal (<1% during collection)
- **Database Impact**: ~100 write operations per collection cycle
- **Memory Usage**: ~50MB additional for collector service

## Troubleshooting Metrics Collection

### Missing Metrics

1. Verify Striim connection:
   - Check `mon system` command manually
   - Verify credentials in configuration

2. Check Splunk connectivity:
   - Verify HEC endpoint is accessible
   - Confirm HEC token is valid

3. Review logs:
   ```
   docker-compose logs backend | grep -i "metric\|error"
   ```

### Incorrect Values

1. Verify metric extraction logic in `MetricsCollectionService`
2. Check Striim API response format
3. Validate transformation in `SplunkHecClient`

### High Latency

1. Reduce collection interval
2. Optimize database queries
3. Check network connectivity

## Extending Metrics

To add new metrics:

1. **Update MetricsCollectionService.java**:
```java
Map<String, Object> metrics = new HashMap<>();
metrics.put("new_metric_name", extractNewMetric());
```

2. **Update documentation** in this file

3. **Test collection**:
```bash
curl -X POST http://localhost:8080/api/v1/collect/trigger \
  -H "Content-Type: application/json" \
  -d '{"targetCommands": ["mon system", "mon apps"]}'
```

4. **Verify in Splunk**:
```spl
sourcetype="striim:metrics" | fields metrics.new_metric_name
```
