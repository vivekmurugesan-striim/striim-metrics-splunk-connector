import React, { useState, useEffect } from 'react';
import { configApi } from '../api/apiClient';

function ConfigPanel() {
  const [isEditMode, setIsEditMode] = useState(false);
  const [formData, setFormData] = useState({
    striimUrl: '',
    striimUser: '',
    striimPassword: '',
    splunkHecUrl: '',
    splunkToken: '',
    splunkIndex: 'striim_app_mon',
    collectionIntervalSeconds: 60,
  });

  const [savedConfig, setSavedConfig] = useState(null);
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(false);
  const [collectionPaused, setCollectionPaused] = useState(false);

  useEffect(() => {
    loadConfiguration();
  }, []);

  const loadConfiguration = async () => {
    try {
      const response = await configApi.getConfig();
      setSavedConfig(response.data);
      setFormData({
        ...response.data,
        striimPassword: '',
        splunkToken: '',
      });
    } catch (error) {
      setMessage({
        type: 'error',
        text: 'Failed to load configuration',
      });
    }
  };

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
        text: 'Configuration saved successfully!',
      });
      setSavedConfig(formData);
      setIsEditMode(false);
      await loadConfiguration();
    } catch (error) {
      setMessage({
        type: 'error',
        text: error.response?.data?.message || 'Failed to save configuration',
      });
    } finally {
      setLoading(false);
    }
  };

  const toggleCollectionPause = () => {
    setCollectionPaused(!collectionPaused);
    setMessage({
      type: 'info',
      text: collectionPaused ? 'Metrics collection resumed' : 'Metrics collection paused',
    });
  };

  const maskSensitiveValue = (value, show = false) => {
    if (!value) return 'Not configured';
    if (show) return value;
    return '●'.repeat(Math.min(value.length, 12));
  };

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h2 className="card-title">Configuration & Status</h2>
        <div style={{ display: 'flex', gap: '10px' }}>
          {isEditMode ? (
            <>
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setIsEditMode(false);
                  loadConfiguration();
                }}
              >
                Cancel
              </button>
              <button
                className="btn btn-primary"
                onClick={handleSubmit}
                disabled={loading}
              >
                {loading ? 'Saving...' : 'Save Changes'}
              </button>
            </>
          ) : (
            <>
              <button
                className="btn btn-secondary"
                onClick={() => setIsEditMode(true)}
              >
                ✎ Edit Configuration
              </button>
              <button
                className={`btn ${collectionPaused ? 'btn-success' : 'btn-warning'}`}
                onClick={toggleCollectionPause}
              >
                {collectionPaused ? '▶ Resume Collection' : '⏸ Pause Collection'}
              </button>
            </>
          )}
        </div>
      </div>

      {message && (
        <div className={`alert alert-${message.type}`}>
          {message.text}
        </div>
      )}

      {collectionPaused && (
        <div style={{ backgroundColor: '#fff3cd', border: '1px solid #ffc107', borderRadius: '4px', padding: '12px', marginBottom: '20px', color: '#856404' }}>
          ⚠️ <strong>Metrics collection is paused.</strong> Click "Resume Collection" to continue.
        </div>
      )}

      {isEditMode ? (
        /* EDIT MODE */
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
                <label className="form-label">Striim Username</label>
                <input
                  type="text"
                  className="form-input"
                  name="striimUser"
                  placeholder="e.g., admin"
                  value={formData.striimUser}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Striim Password</label>
                <input
                  type="password"
                  className="form-input"
                  name="striimPassword"
                  placeholder="Leave blank to keep existing password"
                  value={formData.striimPassword}
                  onChange={handleChange}
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
                  placeholder="http://host.docker.internal:8088/services/collector"
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
                  placeholder="Leave blank to keep existing token"
                  value={formData.splunkToken}
                  onChange={handleChange}
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
                  placeholder="striim_app_mon"
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
        </form>
      ) : (
        /* VIEW MODE */
        <div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
            {/* Striim Configuration */}
            <div>
              <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '12px', color: '#333' }}>
                Striim Configuration
              </h3>
              <div style={{ backgroundColor: '#f8f9fa', padding: '12px', borderRadius: '4px', marginBottom: '12px' }}>
                <div style={{ marginBottom: '12px' }}>
                  <label style={{ fontSize: '12px', color: '#666', fontWeight: 600 }}>Striim URL</label>
                  <div style={{ fontSize: '14px', color: '#333', marginTop: '4px' }}>
                    {savedConfig?.striimUrl || 'Not configured'}
                  </div>
                </div>
                <div style={{ marginBottom: '12px' }}>
                  <label style={{ fontSize: '12px', color: '#666', fontWeight: 600 }}>Username</label>
                  <div style={{ fontSize: '14px', color: '#333', marginTop: '4px' }}>
                    {savedConfig?.striimUser || 'Not configured'}
                  </div>
                </div>
                <div>
                  <label style={{ fontSize: '12px', color: '#666', fontWeight: 600 }}>Password</label>
                  <div style={{ fontSize: '14px', color: '#333', marginTop: '4px' }}>
                    {maskSensitiveValue(savedConfig?.striimPasswordEnc)}
                  </div>
                </div>
              </div>
            </div>

            {/* Splunk Configuration */}
            <div>
              <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '12px', color: '#333' }}>
                Splunk Configuration
              </h3>
              <div style={{ backgroundColor: '#f8f9fa', padding: '12px', borderRadius: '4px', marginBottom: '12px' }}>
                <div style={{ marginBottom: '12px' }}>
                  <label style={{ fontSize: '12px', color: '#666', fontWeight: 600 }}>HEC URL</label>
                  <div style={{ fontSize: '14px', color: '#333', marginTop: '4px', wordBreak: 'break-all' }}>
                    {savedConfig?.splunkHecUrl || 'Not configured'}
                  </div>
                </div>
                <div style={{ marginBottom: '12px' }}>
                  <label style={{ fontSize: '12px', color: '#666', fontWeight: 600 }}>Index</label>
                  <div style={{ fontSize: '14px', color: '#333', marginTop: '4px' }}>
                    {savedConfig?.splunkIndex || 'striim_app_mon'}
                  </div>
                </div>
                <div>
                  <label style={{ fontSize: '12px', color: '#666', fontWeight: 600 }}>HEC Token</label>
                  <div style={{ fontSize: '14px', color: '#333', marginTop: '4px' }}>
                    {maskSensitiveValue(savedConfig?.splunkTokenEnc)}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Collection Settings */}
          <div style={{ marginTop: '20px', paddingTop: '20px', borderTop: '1px solid #eee' }}>
            <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '12px', color: '#333' }}>
              Collection Settings
            </h3>
            <div style={{ backgroundColor: '#f8f9fa', padding: '12px', borderRadius: '4px' }}>
              <div style={{ marginBottom: '12px' }}>
                <label style={{ fontSize: '12px', color: '#666', fontWeight: 600 }}>Collection Interval</label>
                <div style={{ fontSize: '14px', color: '#333', marginTop: '4px' }}>
                  {savedConfig?.collectionIntervalSeconds || 60} seconds ({Math.round((savedConfig?.collectionIntervalSeconds || 60) / 60)} minutes)
                </div>
              </div>
              <div>
                <label style={{ fontSize: '12px', color: '#666', fontWeight: 600 }}>Collection Status</label>
                <div style={{ fontSize: '14px', color: '#333', marginTop: '4px' }}>
                  {collectionPaused ? (
                    <span style={{ color: '#dc3545' }}>⏸ Paused</span>
                  ) : (
                    <span style={{ color: '#28a745' }}>▶ Active</span>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default ConfigPanel;
