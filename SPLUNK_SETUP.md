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

### 2.1 Access HTTP Event Collector Configuration

1. Go to **Settings** → **Data Inputs**
2. Click **HTTP Event Collector** from the list
3. The HEC configuration page will display all configured tokens
4. Look for the **Global Settings** button at the top right of the page
5. If HEC is disabled, you'll see an "Enable" button - click it to enable HEC globally

### 2.2 Configure HEC Global Settings

1. In the HTTP Event Collector page, click **Global Settings** (top right)
2. Configure the following settings:

   | Setting | Recommended Value | Notes |
   |---------|-------------------|-------|
   | **All Tokens** | `Enabled` | Allow token-based authentication |
   | **Default Source Type** | `Metrics` | Use Metrics category (see steps below) |
   | **Default Index** | `striim_metrics` | Target index for metrics |
   | **Default Output Group** | `None` | Use if you have output groups configured |
   | **Use Deployment Server** | `Unchecked` | Unless using deployment server |
   | **Enable SSL** | `Checked` ✓ | **Required for production** |
   | **HTTP Port Number** | `8088` | Standard Splunk HEC port |

   ![Global Settings Dialog](https://user-images.githubusercontent.com/placeholder/splunk-hec-global-settings.png)

3. **Setting Default Source Type**:
   - Click on **"Select Source Type"** dropdown
   - A list of source type categories will appear:
     - Application
     - Database
     - Email
     - **Metrics** ← **Select this**
     - Log to Metrics
     - Miscellaneous
     - Network & Security
     - Operating System
     - Structured
     - Uncategorized
     - Web
   - Select **"Metrics"** as the default source type category
   - You can optionally filter using the search box to find "Metrics" quickly

4. **Setting Default Index**:
   - Click on the **Default Index** dropdown
   - Select or type `striim_metrics`
   - This ensures all events default to this index if not specified in token

5. After enabling **"Enable SSL"**:
   - Select your **SSL Certificate** from the dropdown
   - Ensure the certificate is valid and matches your HEC hostname
   - For self-signed certificates in testing, use `https://` with `-k` flag in curl

6. Click **Save** to apply global settings

### 2.3 SSL Configuration Options

#### Option A: With SSL (Production - Recommended)
- **Enable SSL**: ✓ Checked
- **SSL Certificate**: Select valid certificate
- **HEC URL**: `https://splunk.company.com:8088/services/collector`
- **Port**: 8088

#### Option B: Without SSL (Development/Testing Only - NOT for Production)
- **Enable SSL**: ☐ Unchecked
- **HEC URL**: `http://splunk.company.com:8088/services/collector`
- **Port**: 8088

**⚠️ WARNING**: HTTP without SSL is **NOT recommended for production**. Use only for:
- Local development
- Testing environments
- Networks with existing security controls
- Short-term testing phases

### 2.4 Verify Global Settings Applied

Run this search to confirm settings:
```spl
| rest /services/data/inputs/http | fields title, default_sourcetype, default_index, SSL, port
```

Expected output with SSL enabled:
```
title: striim-connector
default_sourcetype: striim:metrics
default_index: striim_metrics
SSL: 1 (enabled)
port: 8088
```

Expected output with SSL disabled:
```
title: striim-connector
default_sourcetype: striim:metrics
default_index: striim_metrics
SSL: 0 (disabled)
port: 8088
```

## Clarification: Source Type vs Sourcetype

Before creating tokens, understand the difference:

| Setting | Purpose | Level | Example |
|---------|---------|-------|---------|
| **Global Settings "Source Type"** | Category for organizing HEC data | Global | "Metrics", "Log to Metrics", "Database" |
| **Token "Sourcetype"** | Custom identifier for specific data sources | Per-Token | "striim:metrics", "striim:system" |

- **Global Source Type** = How Splunk categorizes the data type (metrics vs logs)
- **Token Sourcetype** = Custom label to identify where data comes from (useful for searches)

For Striim metrics:
- Global Setting: Select **"Metrics"** category
- Token Sourcetype: Use **"striim:metrics"** custom value

This allows searches like: `index=striim_metrics sourcetype=striim:metrics`

## Step 3: Create HEC Token

### 3.1 Create New Token

1. In the HTTP Event Collector section, click **New Token** button
2. Fill in the token details:

   | Field | Value | Description |
   |-------|-------|-------------|
   | **Name** | `striim-connector` | Descriptive name for the token |
   | **Description** | `Token for Striim metrics collection` | Optional description |
   | **Source name override** | `striim` | Identifies the source of events |
   | **Sourcetype** | `striim:metrics` | Custom sourcetype for Striim metrics |
   | **Default Index** | `striim_metrics` | Target index created earlier |

   **Notes:**
   - The **Sourcetype** field allows custom sourcetype creation (e.g., `striim:metrics`)
   - This is different from the Global Settings "Source Type" which is a category
   - Sourcetype helps identify and search for metrics from this specific source
   - The Global Settings "Metrics" category ensures proper handling of metrics data

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

**Option A: Using HTTPS with Valid Certificate (Production)**

```bash
# Replace with your actual values
HEC_URL="https://splunk.company.com:8088/services/collector"
HEC_TOKEN="your-hec-token-value"
INDEX="striim_metrics"

# Send test event (with valid SSL certificate)
curl \
  -H "Authorization: Splunk $HEC_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"event":{"test":"message"},"sourcetype":"striim:metrics","index":"'$INDEX'","time":'$(date +%s)'}' \
  $HEC_URL
```

**Option B: Using HTTPS with Self-Signed Certificate (Testing)**

```bash
# Add -k flag to skip SSL verification (testing only, NOT for production)
HEC_URL="https://splunk.company.com:8088/services/collector"
HEC_TOKEN="your-hec-token-value"
INDEX="striim_metrics"

curl -k \
  -H "Authorization: Splunk $HEC_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"event":{"test":"message"},"sourcetype":"striim:metrics","index":"'$INDEX'","time":'$(date +%s)'}' \
  $HEC_URL
```

**Option C: Using HTTP Without SSL (Development/Testing Only - Recommended for Testing Without Certificate)**

```bash
# ⚠️  HTTP without SSL - NOT recommended for production
# Make sure you disabled SSL in Global Settings first!

HEC_URL="http://splunk.company.com:8088/services/collector"
HEC_TOKEN="your-hec-token-value"
INDEX="striim_metrics"

curl \
  -H "Authorization: Splunk $HEC_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"event":{"test":"message"},"sourcetype":"striim:metrics","index":"'$INDEX'","time":'$(date +%s)'}' \
  $HEC_URL
```

**Steps for HTTP Testing Without SSL:**

1. In Splunk: Go to **Settings** → **Data Inputs** → **HTTP Event Collector** → **Global Settings**
2. Uncheck **"Enable SSL"**
3. Click **Save**
4. Use HTTP URL: `http://splunk.company.com:8088/services/collector` (not https)
5. Run the curl command above (Option C)

**Expected Response:**
```json
{
  "text": "Success",
  "code": 0
}
```

**Error Responses:**
```json
// Invalid token
{
  "text": "Token disabled",
  "code": 4
}

// Invalid index
{
  "text": "Invalid index",
  "code": 10
}

// Connection refused
curl: (7) Failed to connect to splunk.company.com port 8088
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

**For HTTPS (Production):**
```env
# Splunk Configuration with HTTPS/SSL
SPLUNK_HEC_URL=https://splunk.company.com:8088/services/collector
SPLUNK_TOKEN=your-hec-token-value
SPLUNK_INDEX=striim_metrics
```

**For HTTP Without SSL (Development/Testing):**
```env
# Splunk Configuration without SSL (testing only)
# ⚠️  NOT recommended for production
SPLUNK_HEC_URL=http://splunk.company.com:8088/services/collector
SPLUNK_TOKEN=your-hec-token-value
SPLUNK_INDEX=striim_metrics
```

### 5.2 Configure via Web UI

1. Start the Striim Connector application
2. Access the frontend at `http://localhost:3000`
3. Navigate to **Configuration** tab
4. Enter Splunk settings:

   **For HTTPS:**
   - **Splunk HEC URL**: `https://splunk.company.com:8088/services/collector`
   - **Splunk HEC Token**: `your-hec-token-value`
   - **Splunk Index**: `striim_metrics`

   **For HTTP (Testing Only):**
   - **Splunk HEC URL**: `http://splunk.company.com:8088/services/collector`
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

## Docker-Specific Configuration

If you're running Splunk in Docker, follow these additional steps:

### Networking Configuration

**Scenario 1: Striim Connector runs on host machine**

```bash
# Use localhost to connect to Docker Splunk
SPLUNK_HEC_URL=http://localhost:8088/services/collector
```

**Scenario 2: Both Splunk and Striim Connector run in Docker**

Check if they're on the same network:
```bash
# List Docker networks
docker network ls

# Inspect Splunk container's network
docker inspect splunk | grep -A 10 "Networks"

# If on same network, use container name
SPLUNK_HEC_URL=http://splunk:8088/services/collector
```

**Scenario 3: Get container IP address**

```bash
# Get Splunk container IP
SPLUNK_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' splunk)

# Use the IP
SPLUNK_HEC_URL=http://$SPLUNK_IP:8088/services/collector
```

### Port Mapping Verification

Check your `docker ps` output:

```
PORTS                                    DESCRIPTION
0.0.0.0:8000->8000/tcp                  ✓ Port 8000 exposed to host (accessible at localhost:8000)
8088-8089/tcp                            ✗ Port 8088 NOT exposed to host
```

If you need to access HEC from host machine, publish the port:

```bash
# Stop current container
docker stop splunk

# Remove container
docker rm splunk

# Start with port mapping
docker run -d \
  --name splunk \
  -p 8000:8000 \
  -p 8088:8088 \
  -e SPLUNK_PASSWORD=YourPassword \
  -e SPLUNK_START_ARGS='--accept-license' \
  splunk/splunk:latest

# Now accessible from host machine
SPLUNK_HEC_URL=http://localhost:8088/services/collector
```

---

## Quick Start: Testing Without SSL Certificate

If you don't have an SSL certificate, follow these steps to test with HTTP:

### Step 1: Disable SSL in Splunk

1. Go to **Settings** → **Data Inputs** → **HTTP Event Collector**
2. Click **Global Settings** (top right)
3. **Uncheck** the "Enable SSL" checkbox
4. Click **Save**

### Step 2: Update Connector Configuration

Use HTTP URL instead of HTTPS:

```env
SPLUNK_HEC_URL=http://splunk.company.com:8088/services/collector
SPLUNK_TOKEN=your-hec-token-value
SPLUNK_INDEX=striim_metrics
```

### Step 3: Test Connection

```bash
HEC_URL="http://splunk.company.com:8088/services/collector"
HEC_TOKEN="your-token"
INDEX="striim_metrics"

curl \
  -H "Authorization: Splunk $HEC_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"event":{"test":"message"},"sourcetype":"striim:metrics","index":"'$INDEX'","time":'$(date +%s)'}' \
  $HEC_URL
```

### Step 4: Verify in Splunk

```spl
index=striim_metrics sourcetype=striim:metrics
```

### ⚠️ Important Notes

- **Development Only**: HTTP without SSL is fine for development and testing
- **Not for Production**: Always use HTTPS with SSL in production environments
- **When You Get a Certificate**: Simply change the URL from `http://` to `https://` and enable SSL
- **No Code Changes Needed**: Just update the environment variable and restart the connector

---

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

### Issue: Connection Timeout or "Could Not Connect to Server"

**Symptoms**: 
- `curl: (7) Failed to connect to 0.0.0.0 port 8088`
- Connection refused
- Could not connect to server

**Solutions**:

1. **Verify Splunk is running and accessible**:
   ```bash
   # Check Splunk web UI
   curl http://localhost:8000
   
   # Check HEC endpoint
   curl http://localhost:8088
   ```

2. **Correct the HEC URL** (0.0.0.0 is not valid):
   - `0.0.0.0` is not a routable address
   - Use `localhost` or `127.0.0.1` for local access
   - Use container name or IP if in Docker

3. **If Splunk runs in Docker**:
   
   **Option A: Connector on local machine (not Docker)**
   ```bash
   SPLUNK_HEC_URL=http://localhost:8088/services/collector
   ```
   
   **Option B: Connector and Splunk both in Docker**
   ```bash
   # Use container name if on same Docker network
   SPLUNK_HEC_URL=http://splunk:8088/services/collector
   
   # Or get container IP
   docker inspect splunk | grep IPAddress
   SPLUNK_HEC_URL=http://172.17.0.2:8088/services/collector
   ```

4. **Verify port mapping**:
   ```bash
   docker ps | grep splunk
   # Look for: 0.0.0.0:8000->8000/tcp means it's accessible on localhost:8000
   # 8088-8089/tcp means port 8088 is open for container communication
   ```

5. **Check firewall rules**:
   ```bash
   # macOS/Linux firewall
   sudo ufw status
   sudo ufw allow 8088/tcp
   ```

6. **Verify HEC is enabled**:
   - Access Splunk UI: http://localhost:8000
   - Go to **Settings** → **Data Inputs** → **HTTP Event Collector**
   - Status should show "Enabled"
   - If disabled, click "Enable"

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

### Issue: SSL Certificate Errors

**Symptoms**: 
- `curl: (60) SSL certificate problem`
- `javax.net.ssl.SSLHandshakeException`
- `unable to verify the first certificate`

**Solutions**:

1. **If you don't have a certificate**, disable SSL for testing:
   ```bash
   # In Splunk: Settings → Data Inputs → HTTP Event Collector → Global Settings
   # Uncheck "Enable SSL"
   # Use HTTP URL in connector:
   SPLUNK_HEC_URL=http://splunk.company.com:8088/services/collector
   ```

2. **If using self-signed certificate**, use `-k` flag in curl:
   ```bash
   curl -k -H "Authorization: Splunk $HEC_TOKEN" ... $HEC_URL
   ```

3. **Verify certificate is valid**:
   ```bash
   openssl s_client -connect splunk.company.com:8088 -showcerts
   ```

4. **When you get a proper certificate**, switch back to HTTPS:
   ```bash
   SPLUNK_HEC_URL=https://splunk.company.com:8088/services/collector
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
