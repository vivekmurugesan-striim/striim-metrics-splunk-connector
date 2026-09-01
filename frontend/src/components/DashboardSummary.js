import React, { useState, useEffect } from 'react';
import { collectApi, historyApi } from '../api/apiClient';

function DashboardSummary() {
  const [historyData, setHistoryData] = useState(null);
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchHistory();
  }, []);

  const fetchHistory = async () => {
    try {
      const response = await historyApi.getHistory();
      setHistoryData(response.data);
    } catch (error) {
      setMessage({
        type: 'error',
        text: 'Failed to load execution history',
      });
    }
  };

  const handleTriggerCollection = async () => {
    setLoading(true);
    setMessage(null);

    try {
      const response = await collectApi.triggerCollection(['mon system', 'mon apps']);
      setMessage({
        type: 'success',
        text: `Metrics collection triggered. Execution ID: ${response.data.executionId}`,
      });
      setTimeout(fetchHistory, 1000);
    } catch (error) {
      setMessage({
        type: 'error',
        text: 'Failed to trigger metrics collection',
      });
    } finally {
      setLoading(false);
    }
  };

  const recentRuns = historyData?.runs?.slice(0, 5) || [];
  const successCount = historyData?.runs?.filter(r => r.status === 'COMPLETED').length || 0;
  const failedCount = historyData?.runs?.filter(r => r.status === 'FAILED').length || 0;

  return (
    <div>
      {message && (
        <div className={`alert alert-${message.type}`}>
          {message.text}
        </div>
      )}

      {/* Status Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '20px', marginBottom: '20px' }}>
        <div className="card" style={{ textAlign: 'center' }}>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#2e7d32', marginBottom: '8px' }}>
            {successCount}
          </div>
          <div style={{ fontSize: '14px', color: '#666' }}>Successful Collections</div>
        </div>

        <div className="card" style={{ textAlign: 'center' }}>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#c62828', marginBottom: '8px' }}>
            {failedCount}
          </div>
          <div style={{ fontSize: '14px', color: '#666' }}>Failed Collections</div>
        </div>

        <div className="card" style={{ textAlign: 'center' }}>
          <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#1565c0', marginBottom: '8px' }}>
            {historyData?.totalRecords || 0}
          </div>
          <div style={{ fontSize: '14px', color: '#666' }}>Total Runs</div>
        </div>
      </div>

      {/* Action Card */}
      <div className="card">
        <h2 className="card-title">Manual Trigger</h2>
        <p style={{ color: '#666', marginBottom: '16px', fontSize: '14px' }}>
          Trigger a manual collection of system and application metrics
        </p>
        <button
          className="btn btn-primary"
          onClick={handleTriggerCollection}
          disabled={loading}
        >
          {loading ? 'Triggering...' : 'Trigger Metrics Collection'}
        </button>
      </div>

      {/* Recent Runs */}
      <div className="card">
        <h2 className="card-title">Recent Execution History</h2>
        {recentRuns.length === 0 ? (
          <p style={{ color: '#999', fontSize: '14px' }}>No execution history available</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Execution ID</th>
                <th>Status</th>
                <th>Start Time</th>
                <th>Metrics Collected</th>
                <th>Published to Splunk</th>
              </tr>
            </thead>
            <tbody>
              {recentRuns.map(run => (
                <tr key={run.executionId}>
                  <td style={{ fontFamily: 'monospace', fontSize: '12px' }}>{run.executionId}</td>
                  <td>
                    <span className={`status-${run.status.toLowerCase()}`}>
                      {run.status}
                    </span>
                  </td>
                  <td style={{ fontSize: '13px' }}>
                    {run.startTime ? new Date(run.startTime).toLocaleString() : '-'}
                  </td>
                  <td>{run.metricsCollected || '-'}</td>
                  <td>
                    <span className={`badge ${run.publishedToSplunk ? 'badge-success' : 'badge-warning'}`}>
                      {run.publishedToSplunk ? 'Yes' : 'No'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default DashboardSummary;
