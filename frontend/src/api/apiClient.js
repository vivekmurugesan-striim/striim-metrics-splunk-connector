import axios from 'axios';

const API_BASE_URL = '/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const configApi = {
  saveConfig: (configData) =>
    apiClient.post('/v1/config', configData),
  getConfig: () =>
    apiClient.get('/v1/config'),
};

export const collectApi = {
  triggerCollection: (commands) =>
    apiClient.post('/v1/collect/trigger', { targetCommands: commands }),
  getExecutionStatus: (executionId) =>
    apiClient.get(`/v1/collect/status/${executionId}`),
};

export const historyApi = {
  getHistory: () =>
    apiClient.get('/v1/history'),
};

export default apiClient;
