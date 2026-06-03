# Provider Secret Security

Provider credentials are handled as sensitive backend-only data.

## Storage

- Secrets are encrypted at rest by `AiCredentialEncryptionService` using AES/GCM.
- The encryption key comes from `ai.credentials.encryption-key` or `AI_CREDENTIAL_ENCRYPTION_KEY`.
- The key must decode or resolve to 16, 24, or 32 bytes.
- Empty submitted secrets clear the credential only through explicit revoke/update behavior; saved secrets are otherwise preserved when omitted.

## API Safety

- API responses return only status metadata such as `CONFIGURED`, `REVOKED`, `lastTestStatus`, and `secretsHidden`.
- Raw API keys, payment secrets, signed URLs, and encrypted values are not returned to the frontend.
- Test-connection accepts an optional new secret for one-time testing, but does not persist it unless the save endpoint is called.
- Kafka/event payloads include provider id/code/action only, not secrets.

## Authorization

- Provider settings endpoints are MASTER-only.
- The frontend hides controls for non-MASTER users, but backend authorization remains authoritative.

## Logging

Request bodies containing `secret` must not be logged. Any future request logging middleware must redact:
- `secret`
- `apiKey`
- `encryptedSecret`
- `webhookSecret`
- payment provider credentials

## Current Gap

Live OpenAI connection validation is not implemented in this patch. The endpoint returns `NOT_IMPLEMENTED` instead of fake success until a real adapter can safely call provider APIs with timeout and throttling.
