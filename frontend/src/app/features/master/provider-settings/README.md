# Provider Credit Exchange UI

Lebhas treats provider capacity and workspace credits as separate ledgers.

- Master configures AI providers, secure server-side credentials, provider credit pools, and provider-to-Lebhas exchange policies.
- Provider credentials are submitted once through the Master Provider Settings form and displayed only as masked metadata after save.
- Raw provider keys must not be stored in frontend state, browser storage, screenshots, fixtures, examples, or UI copy.
- Admin workspaces see only internal Lebhas credits: available, reserved, used, refunded, free-credit status, purchase CTA, and credit ledger.
- Free signup credit percentage is read from backend exchange policy configuration. The frontend preview calculates convenience estimates only; backend remains source of truth.
- Provider balances, provider pools, exchange policies, and provider credentials are Master-only screens.

Routes:

- `/master/provider-settings`
- `/master/provider-credit-pools`
- `/master/exchange-policies`
- `/master/credit-overview`
- `/credits`
- `/credits/ledger`
