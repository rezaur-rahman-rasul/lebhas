# Provider Settings API

Provider Settings is a MASTER-only backend foundation for configuring AI, payment, storage, and notification providers without exposing secrets to the frontend.

## Endpoints

All endpoints are under `/api/v1/master` and require `hasRole('MASTER')`.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/providers` | List provider metadata and credential status. Optional filters: `type`, `status`, `environment`. |
| POST | `/providers` | Create a provider record. |
| GET | `/providers/{providerId}` | Read provider metadata and credential status. |
| PUT | `/providers/{providerId}` | Update provider metadata. |
| PUT | `/providers/{providerId}/credentials` | Save or rotate a provider credential for `SANDBOX` or `LIVE`. |
| POST | `/providers/{providerId}/test-connection` | Test a submitted secret or saved credential. |
| DELETE | `/providers/{providerId}/credentials?environment=SANDBOX` | Revoke a credential without deleting provider metadata. |
| PATCH | `/providers/{providerId}/status` | Enable or disable a provider. |

## Provider DTO

`GET /providers` returns:

```json
{
  "id": "...",
  "providerCode": "OPENAI",
  "displayName": "OpenAI",
  "providerType": "AI",
  "status": "ACTIVE",
  "credentialStatus": "CONFIGURED",
  "activeEnvironment": "SANDBOX",
  "lastTestStatus": "SUCCESS",
  "lastTestedAt": "...",
  "secretsHidden": true
}
```

Raw provider secrets are never returned.

## Credential Save

`PUT /providers/{providerId}/credentials`

```json
{
  "environment": "SANDBOX",
  "secret": "sk-...",
  "webhookUrl": "https://example.com/webhook",
  "active": true
}
```

Rules:
- `secret` is required for the first credential configuration.
- `secret` is optional for metadata-only updates after a credential exists.
- `webhookUrl` must be valid HTTP or HTTPS when provided.
- The response contains credential status only.

## Connection Testing

`POST /providers/{providerId}/test-connection` can test a submitted secret without persisting it, or test the saved encrypted credential when no secret is submitted.

The current implementation returns `NOT_IMPLEMENTED` for live provider checks until a real provider adapter is wired. It does not fake success.

## Seeded Providers

Migration `V51__provider_settings_credential_workflow.sql` seeds editable backend-driven provider records for OpenAI, Anthropic, Gemini, Stability, SSLCOMMERZ, bKash, and Nagad when missing.
