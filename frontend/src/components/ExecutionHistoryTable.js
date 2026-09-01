import React, { useState, useEffect } from 'react';
import { historyApi } from '../api/apiClient';

function ExecutionHistoryTable() {
  const [history, setHistory] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  useEffect(() => {
    fetchHistory();
  }, []);

  const fetchHistory = async () => {
    try {
      setLoading(true);
      const response = await historyApi.getHistory();
      setHistory(response.data);
      setError(null);
    } catch (err) {
      setError('Failed to load execution history');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const getDuration = (startTime, endTime) => {
    if (!startTime || !endTime) return '-';
    const start = new Date(startTime);
    const end = new Date(endTime);
    const duration = Math.round((end - start) / 1000);
    return `${duration}s`;
  };

  const getStatusBadge = (status) => {
    const statusMap = {
      COMPLETED: 'badge-success',
      RUNNING: 'badge-info',
      FAILED: 'badge-danger',
    };
    return statusMap[status] || 'badge-info';
  };

  if (loading) {
    return <div className="card">Loading execution history...</div>;
  }

  if (error) {
    return (
      <div className="card">
        <div className="alert alert-error">{error}</div>
        <button className="btn btn-primary" onClick={fetchHistory}>
          Retry
        </button>
      </div>
    );
  }

  const runs = history?.runs || [];
  const totalPages = Math.ceil(runs.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const displayedRuns = runs.slice(startIndex, startIndex + itemsPerPage);

  return (
    <div className="card">
      <h2 className="card-title">Execution History</h2>

      {runs.length === 0 ? (
        <p style={{ color: '#999', fontSize: '14px' }}>No execution history available</p>
      ) : (
        <>
          <table className="table">
            <thead>
              <tr>
                <th>Execution ID</th>
                <th>Status</th>
                <th>Trigger Type</th>
                <th>Start Time</th>
                <th>Duration</th>
                <th>Metrics Collected</th>
                <th>Published</th>
              </tr>
            </thead>
            <tbody>
              {displayedRuns.map(run => (
                <tr key={run.executionId}>
                  <td style={{ fontFamily: 'monospace', fontSize: '12px' }}>
                    {run.executionId}
                  </td>
                  <td>
                    <span className={`badge ${getStatusBadge(run.status)}`}>
                      {run.status}
                    </span>
                  </td>
                  <td style={{ fontSize: '13px' }}>
                    {run.triggerType || 'UNKNOWN'}
                  </td>
                  <td style={{ fontSize: '13px' }}>
                    {run.startTime ? new Date(run.startTime).toLocaleString() : '-'}
                  </td>
                  <td style={{ fontSize: '13px' }}>
                    {getDuration(run.startTime, run.endTime)}
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    {run.metricsCollected || '-'}
                  </td>
                  <td>
                    <span className={`badge ${run.publishedToSplunk ? 'badge-success' : 'badge-warning'}`}>
                      {run.publishedToSplunk ? 'Yes' : 'No'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Pagination */}
          {totalPages > 1 && (
            <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'center', gap: '8px' }}>
              <button
                className="btn btn-secondary"
                onClick={() => setCurrentPage(Math.max(1, currentPage - 1))}
                disabled={currentPage === 1}
              >
                Previous
              </button>
              <span style={{ padding: '8px 12px', color: '#666' }}>
                Page {currentPage} of {totalPages}
              </span>
              <button
                className="btn btn-secondary"
                onClick={() => setCurrentPage(Math.min(totalPages, currentPage + 1))}
                disabled={currentPage === totalPages}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}

      <div style={{ marginTop: '20px' }}>
        <button className="btn btn-secondary" onClick={fetchHistory}>
          Refresh
        </button>
      </div>
    </div>
  );
}

export default ExecutionHistoryTable;
