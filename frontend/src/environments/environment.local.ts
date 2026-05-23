import { AppEnvironment } from './environment.model';

export const environment: AppEnvironment = {
  production: false,
  appName: 'Lebhas Creative Maker',
  appVersion: '0.1.0-local',
  apiBaseUrl: '',
  authApiPrefix: '/api/v1/auth',
  workspaceHeaderName: 'X-Workspace-ID',
  correlationIdHeaderName: 'X-Correlation-ID',
};
