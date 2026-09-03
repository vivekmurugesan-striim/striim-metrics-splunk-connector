# Splunk Dashboard for Striim Metrics

This directory contains Splunk dashboards and configuration for monitoring Striim applications using the Striim-Splunk Connector.

## Files

### striim_application_monitor.xml
A comprehensive Splunk dashboard for monitoring Striim applications in real-time using mon command metrics.

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

### Step-by-Step: Method 1 (UI Import - Recommended)

**Best for:** One-time import, visual verification

1. **Open Splunk**
   - Navigate to: `http://localhost:8000` (or your Splunk URL)
   - Log in with admin credentials

2. **Create New Dashboard**
   - Click **Dashboards** (top menu)
   - Click **Create New** → **Dashboard**
   - Enter name: `striim_application_monitor`
   - Select App: `search`
   - Click **Create Dashboard**

3. **Replace Dashboard XML**
   - Click **Edit** (top right of dashboard)
   - Click **Edit Source** (top right)
   - Select ALL existing XML text (Ctrl+A or Cmd+A)
   - Delete it
   
4. **Paste New Dashboard**
   - Open `striim_application_monitor.xml` in a text editor
   - Copy entire contents
   - Paste into the Splunk XML editor
   - Click **Save**

5. **Verify Dashboard**
   - You should see the dashboard with 9 panels
   - All panels should show "No results" if data hasn't been collected yet
   - Once metrics are collected, data will appear automatically

### Method 2: Direct File Copy (Docker)

**Best for:** Automated deployments, scripting

```bash
# Navigate to repository root
cd /Users/vivekmurugesan/Code/Striim-Splunk-Connector

# Copy dashboard XML directly into Splunk container
docker compose cp splunk/striim_application_monitor.xml \
  splunk:/opt/splunk/etc/apps/search/local/data/ui/views/striim_application_monitor.xml

# Restart Splunk to load the new dashboard
docker compose restart splunk

# Verify it's loaded (wait 30 seconds after restart)
sleep 30

# Access dashboard at: http://localhost:8000/en-US/app/search/striim_application_monitor
```

### Method 3: Manual File Placement (Advanced)

**Best for:** Custom Splunk installations

1. **Locate Splunk Views Directory**
   - Local installation: `$SPLUNK_HOME/etc/apps/search/local/data/ui/views/`
   - Docker container: `/opt/splunk/etc/apps/search/local/data/ui/views/`

2. **Copy File**
   ```bash
   cp splunk/striim_application_monitor.xml \
     /path/to/splunk/etc/apps/search/local/data/ui/views/
   ```

3. **Restart Splunk**
   ```bash
   # For Docker
   docker compose restart splunk
   
   # Or native Splunk
   $SPLUNK_HOME/bin/splunk restart
   ```

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
   - Check Splunk: `index=striim_metrics sourcetype=_json | head`

## Verifying Data Flow

### Test 1: Check Raw Events
```spl
index=striim_metrics sourcetype=_json
| head 5
```
Should show JSON events with mon command output.

### Test 2: Verify Application Data
```spl
index=striim_metrics sourcetype=_json
| spath output.striimApplications{}.fullName
| head
```
Should show application names from your Striim instance.

### Test 3: Check Status Counts
```spl
index=striim_metrics sourcetype=_json
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
        <query>index=striim_metrics sourcetype=_json | your search here</query>
      </search>
    </table>
  </panel>
</row>
```

## Troubleshooting

### Dashboard Shows "No Results"

**Check 1: Verify data exists**
```spl
index=striim_metrics
```
If no results, metrics haven't been collected yet.

**Check 2: Verify sourcetype**
```spl
index=striim_metrics sourcetype=_json
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
index=striim_metrics sourcetype=_json
| spath output.striimApplications{}.statusChange
| search "output.striimApplications{}.statusChange"=RUNNING
| spath output.striimApplications{}.fullName
| mvexpand "output.striimApplications{}.fullName"
| rename "output.striimApplications{}.fullName" as app_name
| stats count by app_name
```

### Find Recently Changed Applications
```spl
index=striim_metrics sourcetype=_json
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
index=striim_metrics sourcetype=_json
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
