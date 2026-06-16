import { AppEnvironment } from './environment.model';

export const environment: AppEnvironment = {
  production: false,
  appName: 'Lebhas - Brand Attire',
  appVersion: '0.1.0-dev',
  apiBaseUrl: 'http://localhost:8080',
  authApiPrefix: '/api/v1/auth',
  workspaceHeaderName: 'X-Workspace-ID',
  correlationIdHeaderName: 'X-Correlation-ID',
};
