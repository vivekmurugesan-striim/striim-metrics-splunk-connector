# Splunk Configuration Guide

This guide provides step-by-step instructions for configuring Splunk to receive metrics from the Striim Splunk Connector.

## Prerequisites

- Splunk Enterprise or Cloud instance with administrator access
- Splunk version 8.0 or higher
- Network connectivity from the Striim Connector to the Splunk HEC endpoint

## Overview

The Striim Splunk Connector uses Splunk's HTTP Event Collector (HEC) to send metrics. You will need to:

1. Create a Splunk index for storing metrics
2. Configure HTTP Event Collector (HEC)
3. Create an HEC token
4. Update the connector configuration

## Step 1: Create a Splunk Index

### 1.1 Access Index Management

1. Log in to Splunk Web as an administrator
2. Click **Settings** in the top navigation menu
3. Select **Indexes** under Data Inputs

### 1.2 Create New Index

1. Click the **New Index** button
2. Fill in the following details:

   | Field | Value | Description |
   |-------|-------|-------------|
   | **Index Name** | `striim_metrics` | Name for storing Striim metrics |
   | **Data Type** | `Metrics` | Select "Metrics" for time-series data |
   | **Max KB/s** | (optional) | Maximum data rate; leave blank for no limit |
   | **Max Global Raw Size MB** | (optional) | Maximum raw size; leave blank for unlimited |
   | **Max Memory MB** | `20` | Memory limit for index (recommend 20-50) |
   | **Enable IPv6** | Unchecked | Unless you use IPv6 |
   | **Homogeneous Column Datatype** | Checked | Improves compression for metrics |

3. Advanced settings (optional):
   - **Summary homogeneous** - Checked (improves summary searches)
   - **Datatype** - Select "Metrics"
   - **Search String** - Leave blank

4. Click **Save Index**

### 1.3 Verify Index Creation

```spl
| rest /services/data/indexes | search title=striim_metrics | table title, dataType, maxKBps
```

## Step 2: Configure HTTP Event Collector (HEC)

### 2.1 Enable HEC (if not already enabled)

1. Go to **Settings** → **Data Inputs**
2. Click **HTTP Event Collector**
3. If disabled, click **Enable** in the status bar

### 2.2 Configure HEC Global Settings

1. In the HTTP Event Collector page, click **Global Settings** (top right)
2. Configure the following:

   | Setting | Recommended Value | Notes |
   |---------|-------------------|-------|
   | **Max Content Length** | `10000000` (10 MB) | For batch submissions |
   | **Max Number of Tokens** | `100` | Maximum tokens allowed |
   | **Max Sockets** | `1000` | Concurrent connections |
   | **Per-token throughput** | `1000` MB/s | Per token limit |
   | **Enable SSL** | Checked | Required for production |
   | **SSL Certificate** | Select certificate | Use valid certificate |
   | **SSL Key** | Select key file | Corresponding private key |
   | **Require Valid Certificate** | Checked | Recommended |

3. Click **Save** to apply global settings

## Step 3: Create HEC Token

### 3.1 Create New Token

1. In the HTTP Event Collector section, click **New Token** button
2. Fill in the token details:

   | Field | Value | Description |
   |-------|-------|-------------|
   | **Name** | `striim-connector` | Descriptive name for the token |
   | **Description** | `Token for Striim metrics collection` | Optional description |
   | **Source name override** | `striim` | Identifies the source |
   | **Sourcetype** | `striim:metrics` | Categorizes the data |
   | **Default Index** | `striim_metrics` | Target index created earlier |

3. Click **Next** to proceed to allowed indexes

### 3.2 Set Allowed Indexes

1. In the "Select Allowed Indexes" section:
   - Check the box for `striim_metrics`
   - Optionally add other indexes if needed

2. Click **Next** to review and save

### 3.3 Review Token Configuration

The token page displays:
- **Token Value** - The actual HEC token (copy and save this)
- **HEC URL** - The endpoint URL (e.g., `https://splunk.company.com:8088/services/collector`)
- **Status** - Should show "Enabled"

### 3.4 Copy Token Information

**Important**: Save the following information securely:

1. **HEC Token** - Click the eye icon to reveal and copy
2. **HEC URL** - Note the full URL including protocol and port
3. Store in a secure location (password manager, vault, etc.)

## Step 4: Test HEC Connectivity

### 4.1 Test from Command Line

```bash
# Replace with your actual values
HEC_URL="https://splunk.company.com:8088/services/collector"
HEC_TOKEN="your-hec-token-value"
INDEX="striim_metrics"

# Send test event
curl -k \
  -H "Authorization: Splunk $HEC_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"event":{"test":"message"},"sourcetype":"striim:metrics","index":"'$INDEX'","time":'$(date +%s)'}' \
  $HEC_URL
```

Expected response:
```json
{
  "text": "Success",
  "code": 0
}
```

### 4.2 Verify Data in Splunk

1. Go to **Search & Reporting**
2. Run the search:
   ```spl
   index=striim_metrics sourcetype=striim:metrics | tail 1
   ```
3. You should see your test event

## Step 5: Configure Striim Connector

### 5.1 Update Environment Variables

Update your `.env` file:

```env
# Splunk Configuration
SPLUNK_HEC_URL=https://splunk.company.com:8088/services/collector
SPLUNK_TOKEN=your-hec-token-value
SPLUNK_INDEX=striim_metrics
```

### 5.2 Configure via Web UI

1. Start the Striim Connector application
2. Access the frontend at `http://localhost:3000`
3. Navigate to **Configuration** tab
4. Enter Splunk settings:
   - **Splunk HEC URL**: `https://splunk.company.com:8088/services/collector`
   - **Splunk HEC Token**: `your-hec-token-value`
   - **Splunk Index**: `striim_metrics`
5. Click **Save Configuration**

## Step 6: Monitor Metrics Flow

### 6.1 Real-time Monitoring

```spl
index=striim_metrics sourcetype=striim:metrics
| stats count, latest(_time) by host
```

### 6.2 Metrics Dashboard

Create a dashboard to monitor Striim metrics:

```spl
<dashboard>
  <label>Striim Metrics</label>
  <row>
    <panel>
      <title>Metrics Collection Status</title>
      <single>
        <search>
          <query>index=striim_metrics sourcetype=striim:metrics 
          | stats latest(_time) as last_collection
          | eval age=now()-last_collection
          | eval status=if(age &lt; 120, "Active", "Inactive")
          | table status</query>
          <earliest>-7d</earliest>
        </search>
      </single>
    </panel>
    <panel>
      <title>Events per Hour</title>
      <chart>
        <search>
          <query>index=striim_metrics sourcetype=striim:metrics
          | timechart count</query>
          <earliest>-24h</earliest>
        </search>
      </chart>
    </panel>
  </row>
</dashboard>
```

### 6.3 HEC Status Monitoring

Monitor HEC health:

```spl
index=_internal group=queue name=httpeventcollector
| stats avg(current_size_kb) as avg_queue_size, max(current_size_kb) as max_queue_size
```

## Troubleshooting

### Issue: "Unauthorized" Error

**Symptoms**: HTTP 401 response from HEC

**Solutions**:
1. Verify token value is correct (no extra spaces)
2. Confirm token is enabled in Splunk
3. Check token is assigned to the correct index
4. Ensure token hasn't been deleted or revoked

```bash
# Verify token in Splunk
curl -k -u admin:password \
  https://splunk.company.com:8089/services/data/indexes/striim_metrics/data-inputs/http \
  | grep -i disabled
```

### Issue: "Index Does Not Exist"

**Symptoms**: HTTP 400 error mentioning index

**Solutions**:
1. Verify index name matches exactly (case-sensitive)
2. Confirm index `striim_metrics` exists:
   ```spl
   | rest /services/data/indexes | search title=striim_metrics
   ```
3. Ensure token is assigned to this index

### Issue: Connection Timeout

**Symptoms**: Connection refused or timeout

**Solutions**:
1. Verify HEC URL is correct and accessible:
   ```bash
   curl -I https://splunk.company.com:8088
   ```
2. Check firewall rules allow port 8088
3. Confirm Splunk is running and HEC is enabled
4. Check SSL certificate validity if using HTTPS

### Issue: Events Not Appearing in Splunk

**Symptoms**: Connector sends events but they don't appear

**Solutions**:
1. Verify index exists and HEC token can write to it
2. Check event timestamp is correct (within searchable time)
3. Search with earliest time:
   ```spl
   index=striim_metrics sourcetype=striim:metrics earliest=-1h
   ```
4. Check HEC input queue:
   ```spl
   index=_internal group=queue name=httpeventcollector
   | table host, name, current_size
   ```

### Issue: High Latency or Dropped Events

**Symptoms**: Slow data ingestion or missing events

**Solutions**:
1. Increase HEC throughput limits in global settings
2. Implement batching in the connector
3. Monitor HEC queue depth:
   ```spl
   index=_internal group=queue name=httpeventcollector
   | timechart max(current_size_kb) by host
   ```
4. Check Splunk indexing performance:
   ```spl
   index=_internal group=thruput
   | timechart sum(kb) by group
   ```

## Security Best Practices

### 1. Token Rotation

Rotate HEC tokens regularly (quarterly minimum):
- Create new token
- Update connector configuration
- Disable old token after verification
- Delete old token after 30 days

### 2. SSL/TLS Configuration

1. Use valid SSL certificate (not self-signed in production)
2. Enable "Require Valid Certificate" in HEC global settings
3. Configure appropriate TLS version (1.2 or higher)

```bash
# Test SSL
openssl s_client -connect splunk.company.com:8088 -showcerts
```

### 3. Network Security

- Restrict HEC access to known sources (firewall rules)
- Use private network connections when possible
- Monitor HEC access logs:
  ```spl
  index=_internal source=*http_event_collector.log*
  | stats count by status, src_ip
  ```

### 4. Token Management

- Use descriptive token names for tracking
- Assign minimal required permissions
- Monitor token usage:
  ```spl
  index=_internal group=splunk_http_server connection=* 
  | search request | stats count by client_ip, request
  ```

## Advanced Configuration

### Batch Event Submission

For better performance with high volume metrics:

```json
{
  "event": [
    {
      "timestamp": 1234567890,
      "metric_name:cpu_utilization": 45.5,
      "metric_name:memory_usage": 72.3,
      "host": "striim-server-01"
    },
    {
      "timestamp": 1234567891,
      "metric_name:cpu_utilization": 46.2,
      "metric_name:memory_usage": 73.1,
      "host": "striim-server-01"
    }
  ],
  "sourcetype": "striim:metrics",
  "index": "striim_metrics"
}
```

### Custom Metadata

Add custom fields to events:

```json
{
  "event": {
    "cpu_utilization": 45.5,
    "pipeline": "prod-pipeline",
    "datacenter": "us-east-1"
  },
  "sourcetype": "striim:metrics",
  "index": "striim_metrics",
  "host": "striim-server-01",
  "source": "striim-connector",
  "time": 1234567890
}
```

## Performance Tuning

### For High Volume Metrics (>1000 events/sec)

1. Increase HEC max content length:
   ```spl
   Settings → Data Inputs → HTTP Event Collector → Global Settings
   Max Content Length: 50000000 (50 MB)
   ```

2. Implement bulk indexing:
   - Batch events (100-1000 per request)
   - Use JSON array format
   - Implement circuit breakers for backpressure

3. Monitor indexing performance:
   ```spl
   index=_internal group=thruput series=striim_metrics
   | timechart sum(kb)
   ```

### Connection Pooling

Configure connection pool in the Striim Connector:
- Adjust thread pool size
- Implement connection reuse
- Configure timeout values appropriately

## Validation Checklist

Before going to production, verify:

- [ ] Index `striim_metrics` exists and is configured for metrics
- [ ] HEC is enabled globally in Splunk
- [ ] HEC token created with proper permissions
- [ ] Token assigned to `striim_metrics` index
- [ ] SSL certificate is valid and properly configured
- [ ] Firewall allows traffic on port 8088
- [ ] Test event successfully ingested
- [ ] Metrics visible in search within 1-2 minutes
- [ ] Backup plan for token rotation documented
- [ ] Monitoring and alerting configured

## Support and Troubleshooting

### Useful Splunk Commands

**Check HEC status**:
```spl
| rest /services/data/inputs/http/striim-connector
```

**Monitor HEC performance**:
```spl
index=_internal source=*http_event_collector.log* 
| stats count by status, clientip
```

**View recent errors**:
```spl
index=_internal group=splunk_http_server ERROR
| stats count by error
```

**Check token metrics**:
```spl
index=_internal token=*striim*
| stats count, latest(_time) by token, status
```

### Documentation References

- [Splunk HTTP Event Collector Documentation](https://docs.splunk.com/Documentation/Splunk/latest/Data/HEC)
- [Splunk Metrics Data Type](https://docs.splunk.com/Documentation/Splunk/latest/Metrics/GetStarted)
- [Splunk Data Inputs](https://docs.splunk.com/Documentation/Splunk/latest/Data/Aboutdatainputs)

---

**Last Updated**: September 1, 2026  
**Version**: 1.0
