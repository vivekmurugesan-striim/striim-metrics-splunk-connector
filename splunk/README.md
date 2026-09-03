# Splunk Dashboard for Striim Metrics

This directory contains Splunk dashboards and configuration for monitoring Striim applications using the Striim-Splunk Connector.

## Files

### striim_application_monitor.xml
A comprehensive Splunk dashboard for monitoring Striim applications in real-time using mon command metrics (Classic Dashboard using Simple XML format).

**Dashboard Features:**
- **Running Applications Count** - Real-time count of Striim applications with RUNNING status
- **Stopped Applications Count** - Real-time count of applications with STOPPED status
- **Created Applications Count** - Applications in CREATED state (not yet running)
- **Total Applications** - Total number of applications in Striim
- **Applications Status Table** - Detailed view with application name, status, rate, and server count
- **Status Distribution Pie Chart** - Visual breakdown of application statuses
- **Status Breakdown Table** - Count and percentage for each status
- **Metrics Collection Timeline** - When metrics were collected from Striim
- **Recent Events** - Raw JSON events for debugging

**Key Features:**
- 9 interactive panels with time range picker
- Color-coded status indicators (Green=RUNNING, Red=STOPPED, Blue=CREATED)
- Dynamic calculations based on actual mon command response
- 24-hour time range by default (customizable)

## Data Format

The dashboard expects metrics data in this JSON structure:
```json
{
  "command": "mon;",
  "executionStatus": "Success",
  "output": {
    "striimApplications": [
      {
        "entityType": "APPLICATION",
        "fullName": "admin.ApplicationName",
        "statusChange": "RUNNING",
        "rate": "100",
        "sourceRate": "50",
        "cpuRate": "45%",
        "numServers": "2",
        "latestActivity": "2026-09-03 15:03:25"
      }
    ]
  }
}
```

**Supported Application Statuses:**
- **RUNNING** - Application is actively running
- **STOPPED** - Application has been stopped
- **CREATED** - Application exists but hasn't been started yet

## How to Import the Dashboard

**Important:** Splunk does NOT provide a direct "Import XML" button in the UI for Classic dashboards. Use one of the methods below instead (per official Splunk documentation).

### ✅ Method 1: Direct File Copy (RECOMMENDED)

**Best for:** Everyone - simplest, most reliable

This works because Splunk automatically recognizes XML files placed in the `/local/data/ui/views/` directory.

```bash
# Navigate to repository root
cd /Users/vivekmurugesan/Code/Striim-Splunk-Connector

# Copy dashboard XML to Splunk's views directory
docker compose cp splunk/striim_application_monitor.xml \
  splunk:/opt/splunk/etc/apps/search/local/data/ui/views/striim_application_monitor.xml

# Restart Splunk (optional but recommended)
docker compose restart splunk
sleep 30
```

**Access the dashboard:**
- URL: `http://localhost:8000/en-US/app/search/striim_application_monitor`
- Or go to **Dashboards** in Splunk UI and search for "striim_application_monitor"

### Method 2: REST API / Command Line

**Best for:** Automated deployments and scripting

Using Splunk's REST API (per official Splunk documentation):

```bash
# Create dashboard from XML using REST API
curl -X POST "http://localhost:8000/servicesNS/admin/search/data/ui/views" \
  -u admin:changeme \
  -d "name=striim_application_monitor" \
  -d "eai:data=$(cat splunk/striim_application_monitor.xml)" \
  -d "app=search"
```

Or using Splunk CLI:

```bash
docker compose exec splunk bash -c \
  "/opt/splunk/bin/splunk create view striim_application_monitor \
   -auth admin:changeme \
   -app search"
```

### Method 3: Manual Edit in Splunk UI

**Best for:** If you want to verify in UI before saving

1. **Create a temporary dashboard**
   - Log in to Splunk: `http://localhost:8000`
   - Click **Dashboards** (top menu)
   - Look for an option to create or import
   - Create any dashboard (can delete later)

2. **Edit the source**
   - Open your dashboard
   - Click **Edit** (top right)
   - Click the **source code/XML icon** in the toolbar
   - A source editor will open

3. **Paste the XML**
   - Clear all existing content
   - Open `splunk/striim_application_monitor.xml` in a text editor
   - Copy the entire contents
   - Paste into Splunk's source editor
   - Click **Save**

4. **Rename dashboard (optional)**
   - Dashboard settings → Change name to `striim_application_monitor`

## Prerequisites

Before the dashboard will display data, ensure:

1. **Backend Service Running**
   ```bash
   docker compose ps | grep backend
   # Should show: striim-connector-backend ... Up
   ```

2. **Splunk HEC Configured**
   - Log into Splunk
   - Settings → Data Inputs → HTTP Event Collector
   - Create a token named `striim_metrics` or update the connector config

3. **Application Configured**
   - Open http://localhost:3000
   - Configure Striim URL, username, password
   - Configure Splunk HEC URL and token
   - Click **Save Configuration**

4. **Metrics Collected**
   - Dashboard → Click **Trigger Metrics Collection**
   - Wait for success message
   - Check Splunk: `index=striim_app_mon sourcetype=_json | head`

## Verifying Data Flow

### Test 1: Check Raw Events
```spl
index=striim_app_mon sourcetype=_json
| head 5
```
Should show JSON events with mon command output.

### Test 2: Verify Application Data
```spl
index=striim_app_mon sourcetype=_json
| spath output.striimApplications{}.fullName
| head
```
Should show application names from your Striim instance.

### Test 3: Check Status Counts
```spl
index=striim_app_mon sourcetype=_json
| spath output.striimApplications{}.statusChange
| mvexpand "output.striimApplications{}.statusChange"
| rename "output.striimApplications{}.statusChange" as status
| stats count by status
```
Should show counts for RUNNING, STOPPED, CREATED statuses.

## Dashboard Customization

### Modify Dashboard
1. Open the dashboard in Splunk
2. Click **Edit** → **Edit Panels** or **Edit Source**
3. Make changes (e.g., change time range, add new panels)
4. Click **Save**

### Export Modified Dashboard
1. Click **Edit** → **Edit Source**
2. Copy all XML text
3. Paste into a text editor
4. Save as new file (e.g., `striim_dashboard_custom.xml`)

### Add Custom Panels
Edit the XML to add new panels. Example structure:
```xml
<row>
  <panel>
    <title>Your Panel Title</title>
    <table>
      <search>
        <query>index=striim_app_mon sourcetype=_json | your search here</query>
      </search>
    </table>
  </panel>
</row>
```

## Troubleshooting

### Dashboard Shows "No Results"

**Check 1: Verify data exists**
```spl
index=striim_app_mon
```
If no results, metrics haven't been collected yet.

**Check 2: Verify sourcetype**
```spl
index=striim_app_mon sourcetype=_json
```
Should return results. If not, check Splunk token configuration.

**Check 3: Check backend logs**
```bash
docker compose logs backend | grep -i "splunk\|error"
```
Look for publishing errors.

### Dashboard Has Old/Incorrect Queries

**Solution:** Re-import the dashboard with the latest XML file
- This ensures all queries use correct field names and structure
- Don't manually edit if you're updating from new versions

### Dashboard Panels Load Slowly

**Cause:** Large time range with many metrics
**Solutions:**
- Reduce time range (default is 24h, try 4h or 1h)
- Index more efficiently (add `earliest=-24h@h` to filter)
- Increase search concurrency in Splunk settings

### XML Import Fails

**Possible causes:**
1. **Syntax error in XML** - Validate in text editor or XML validator
2. **Special characters** - Ensure file is UTF-8 encoded
3. **Path issues** - Ensure dashboard filename matches dashboard name

**Solution:**
1. Validate XML: `xmllint striim_application_monitor.xml` (if available)
2. Try creating a blank dashboard and pasting XML in parts
3. Check Splunk logs: `docker compose logs splunk | grep -i "error"`

## Common Searches

### Find All Running Applications
```spl
index=striim_app_mon sourcetype=_json
| spath output.striimApplications{}.statusChange
| search "output.striimApplications{}.statusChange"=RUNNING
| spath output.striimApplications{}.fullName
| mvexpand "output.striimApplications{}.fullName"
| rename "output.striimApplications{}.fullName" as app_name
| stats count by app_name
```

### Find Recently Changed Applications
```spl
index=striim_app_mon sourcetype=_json
| spath output.striimApplications{}
| mvexpand "output.striimApplications{}"
| rename "output.striimApplications{}.fullName" as app,
         "output.striimApplications{}.statusChange" as status
| dedup app
| sort - _time
| table _time, app, status
```

### Alert When Application Stops
```spl
index=striim_app_mon sourcetype=_json
| spath output.striimApplications{}.statusChange
| search "output.striimApplications{}.statusChange"=STOPPED
| spath output.striimApplications{}.fullName
| mvexpand "output.striimApplications{}.fullName"
| rename "output.striimApplications{}.fullName" as app_name
| stats count by app_name
| where count > 0
```

## Support

For issues with:
- **Connector metrics collection**: Check backend logs
- **Splunk configuration**: Verify HEC token and index settings
- **Dashboard display**: Check Splunk Search & Reporting app
- **XML validation**: Use online XML validators

## Next Steps

1. ✅ Import the dashboard
2. ✅ Configure Striim credentials in application UI
3. ✅ Trigger metrics collection manually
4. ✅ Verify data appears in dashboard
5. ✅ Set up Splunk alerts for status changes
6. ✅ Add dashboard to Splunk home page
7. ✅ Share with team monitoring Striim
