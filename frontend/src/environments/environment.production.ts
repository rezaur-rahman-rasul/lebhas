import { AppEnvironment } from './environment.model';

export const environment: AppEnvironment = {
  production: true,
  appName: 'Lebhas - Brand Attire',
  appVersion: '0.1.0',
  apiBaseUrl: 'https://api.lebhas.com/api/v1',
  authApiPrefix: '/api/v1/auth',
  workspaceHeaderName: 'X-Workspace-ID',
  correlationIdHeaderName: 'X-Correlation-ID',
};
