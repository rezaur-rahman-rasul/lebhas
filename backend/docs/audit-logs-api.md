# Master Audit Logs API

`GET /api/v1/master/audit-logs`

Supported query params: `module`, `action`, `actor`, `severity`, `from`, `to`, `page`, `size`.

Returns `200 OK` with paginated empty results when no logs match. Invalid date ranges return `400 Bad Request`. Sensitive metadata must remain masked by audit mapping and storage policies.
