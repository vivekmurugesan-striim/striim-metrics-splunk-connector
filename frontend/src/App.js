import React, { useState } from 'react';
import './App.css';
import AppHeader from './components/AppHeader';
import NavigationBar from './components/NavigationBar';
import ConfigPanel from './components/ConfigPanel';
import DashboardSummary from './components/DashboardSummary';
import ExecutionHistoryTable from './components/ExecutionHistoryTable';

function App() {
  const [activeTab, setActiveTab] = useState('dashboard');

  return (
    <div className="app">
      <AppHeader />
      <NavigationBar activeTab={activeTab} setActiveTab={setActiveTab} />
      <main className="app-content">
        {activeTab === 'dashboard' && <DashboardSummary />}
        {activeTab === 'config' && <ConfigPanel />}
        {activeTab === 'history' && <ExecutionHistoryTable />}
      </main>
    </div>
  );
}

export default App;
