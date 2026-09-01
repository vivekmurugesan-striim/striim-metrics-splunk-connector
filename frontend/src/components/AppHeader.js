import React from 'react';

function AppHeader() {
  return (
    <div className="app-header">
      <h1>Striim Splunk Connector</h1>
      <p style={{ margin: '5px 0 0 0', opacity: 0.9 }}>
        Monitor and manage metrics collection from Striim to Splunk
      </p>
    </div>
  );
}

export default AppHeader;
