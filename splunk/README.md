# Splunk Dashboard for Striim Metrics

This directory contains Splunk dashboards and configuration for monitoring Striim applications.

## Files

### striim_application_monitor.xml
A comprehensive Splunk dashboard for monitoring Striim applications in real-time.

**Dashboard Features:**
- **Running Applications Count** - Real-time count of running Striim applications
- **Stopped Applications Count** - Real-time count of stopped Striim applications
- **Total Applications** - Total number of applications being monitored
- **Last Collection Time** - When metrics were last collected from Striim
- **Applications Status Table** - Current status of all applications
- **Application Status Over Time** - Trends showing how application status changes over 24 hours
- **Metrics Collections Over Time** - Frequency of metrics collection
- **Status Distribution Pie Chart** - Visual distribution of running vs stopped apps
- **Recent Metrics Events** - Raw data view of the latest metric events

**Panels:**
- 8 interactive panels with time range picker
- Color-coded status indicators (Green=Running, Red=Stopped)
- 24-hour trends and timeline visualizations

## How to Import into Splunk

### Method 1: Using Splunk UI

1. Log in to Splunk: `http://localhost:8000`
2. Go to **Settings → Dashboards**
3. Click **Create New → Dashboard**
4. Name it `striim_application_monitor` and click **Create Dashboard**
5. Close the new dashboard
6. Go back to **Settings → Dashboards** and find `striim_application_monitor`
7. Click on it, then click **Edit → Edit Source**
8. Delete all existing XML and paste the contents of `striim_application_monitor.xml`
9. Click **Save**

### Method 2: Direct File Upload (Docker)

```bash
# From the repository root directory
docker compose cp splunk/striim_application_monitor.xml splunk:/opt/splunk/etc/apps/search/local/data/ui/views/striim_application_monitor.xml

# Restart Splunk to load the dashboard
docker compose restart splunk
```

### Method 3: Splunk CLI

```bash
# Inside the Splunk container
docker compose exec splunk bash -c "cp /host/path/striim_application_monitor.xml /opt/splunk/etc/apps/search/local/data/ui/views/"
docker compose restart splunk
```

## Prerequisites

Before using the dashboard, ensure:

1. **Splunk is running** and HEC (HTTP Event Collector) is configured
2. **striim_metrics index** exists in Splunk
3. **Metrics are being collected** from Striim application
4. **Data is being published** to Splunk via the connector

Test that data is flowing:
```bash
# In Splunk Search & Reporting
index=striim_metrics sourcetype=_json
```

## Dashboard Requirements

The dashboard expects metrics data with the following fields:
- `totalApplications` - Total count of applications
- `runningApplications` - Count of running applications
- `stoppedApplications` - Count of stopped applications
- `applications{}` - JSON array of application objects with `name` and `status` fields

These fields are automatically populated by the `MetricsCollectionService` when it publishes metrics.

## Customizing the Dashboard

To customize the dashboard:

1. Import the XML as described above
2. Click **Edit** on the dashboard
3. Modify panels, add new visualizations, or change time ranges
4. Click **Save**
5. To export modified dashboard back to XML: **Edit Source** and copy the XML

## Troubleshooting

**No data appears in dashboard:**
- Verify metrics are being collected: Run search `index=striim_metrics`
- Check Splunk HEC token is enabled and configured
- Ensure `striim_metrics` index exists
- Verify sourcetype is set to `_json`

**Wrong data format:**
- Dashboard expects the parsed JSON format from `MetricsCollectionService.parseMonResponse()`
- Ensure backend has been rebuilt with the latest code that includes parsing

**Dashboard won't import:**
- Validate XML syntax in a text editor
- Ensure no special characters are breaking the XML structure
- Try creating a new dashboard manually and pasting the XML piece by piece

## Next Steps

1. Import the dashboard
2. Verify metrics are visible
3. Create alerts based on application status changes
4. Add custom searches for specific applications
5. Share the dashboard with your monitoring team
