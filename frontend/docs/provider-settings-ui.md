# Provider Settings UI

Provider Settings is the MASTER-only UI for creating provider records, rotating credentials, testing provider setup, and moving configured providers into Provider Routing.

## User Flow

1. Open `AI Operations -> Provider Settings`.
2. Review the provider list.
3. Click `Add Provider` when a provider record is missing.
4. Create a provider with code such as `OPENAI`, type `AI`, and the supported environments.
5. Select the provider from the list.
6. Choose `Sandbox` or `Live`.
7. Paste a provider secret.
8. Click `Test connection`.
9. Save the credential.
10. Use `Go to Provider Routing` after save to assign the provider to routing policies.

## UI Rules

- Saved secrets are never displayed again.
- Secret inputs use password fields and are cleared after save.
- No provider data is faked; empty provider lists render an empty state.
- Provider list wording is provider-neutral: AI and payment providers are separate backend-driven types.
- OpenAI-specific guidance is shown only when `providerCode` is `OPENAI`.
- Errors are shown in the credential panel and one deduplicated toast per user action.

## API Service

The UI uses `MasterProviderService` only:

- `listProviders`
- `createProvider`
- `getProvider`
- `saveCredential`
- `testConnection`
- `revokeCredential`
- `updateProviderStatus`

The service uses the standard `ApiResponse<T>` wrapper and skips generic HTTP error toasts so the page can show contextual errors.

## Known Limitation

The backend connection test endpoint currently reports `NOT_IMPLEMENTED` until a real OpenAI/provider adapter is implemented. The UI surfaces that status without claiming a successful connection.
