import React, { useState } from 'react';
import { configApi } from '../api/apiClient';

function ConfigPanel() {
  const [formData, setFormData] = useState({
    striimUrl: '',
    striimToken: '',
    splunkHecUrl: '',
    splunkToken: '',
    splunkIndex: 'striim_metrics',
    collectionIntervalSeconds: 60,
  });

  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: name === 'collectionIntervalSeconds' ? parseInt(value) : value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage(null);

    try {
      const response = await configApi.saveConfig(formData);
      setMessage({
        type: 'success',
        text: response.data.message,
      });
    } catch (error) {
      setMessage({
        type: 'error',
        text: error.response?.data?.message || 'Failed to save configuration',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card">
      <h2 className="card-title">Configuration Settings</h2>

      {message && (
        <div className={`alert alert-${message.type}`}>
          {message.text}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
          {/* Striim Configuration */}
          <div>
            <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '12px', color: '#333' }}>
              Striim Configuration
            </h3>

            <div className="form-group">
              <label className="form-label">Striim URL</label>
              <input
                type="text"
                className="form-input"
                name="striimUrl"
                placeholder="http://localhost:9080"
                value={formData.striimUrl}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Striim API Token</label>
              <input
                type="password"
                className="form-input"
                name="striimToken"
                placeholder="Your Striim API token"
                value={formData.striimToken}
                onChange={handleChange}
                required
              />
            </div>
          </div>

          {/* Splunk Configuration */}
          <div>
            <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '12px', color: '#333' }}>
              Splunk Configuration
            </h3>

            <div className="form-group">
              <label className="form-label">Splunk HEC URL</label>
              <input
                type="text"
                className="form-input"
                name="splunkHecUrl"
                placeholder="https://splunk.company.com:8088/services/collector"
                value={formData.splunkHecUrl}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Splunk HEC Token</label>
              <input
                type="password"
                className="form-input"
                name="splunkToken"
                placeholder="Your Splunk HEC token"
                value={formData.splunkToken}
                onChange={handleChange}
                required
              />
            </div>
          </div>
        </div>

        {/* Collection Settings */}
        <div style={{ marginTop: '20px', paddingTop: '20px', borderTop: '1px solid #eee' }}>
          <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '12px', color: '#333' }}>
            Collection Settings
          </h3>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
            <div className="form-group">
              <label className="form-label">Splunk Index</label>
              <input
                type="text"
                className="form-input"
                name="splunkIndex"
                placeholder="striim_metrics"
                value={formData.splunkIndex}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Collection Interval (seconds)</label>
              <input
                type="number"
                className="form-input"
                name="collectionIntervalSeconds"
                placeholder="60"
                value={formData.collectionIntervalSeconds}
                onChange={handleChange}
                min="10"
                required
              />
            </div>
          </div>
        </div>

        <div style={{ marginTop: '20px' }}>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading}
          >
            {loading ? 'Saving...' : 'Save Configuration'}
          </button>
        </div>
      </form>
    </div>
  );
}

export default ConfigPanel;
