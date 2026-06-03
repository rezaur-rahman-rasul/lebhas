# Master AI Operations API Contracts

All endpoints require a MASTER JWT and return the shared `ApiResponse` envelope:

```json
{
  "success": true,
  "message": "Loaded successfully",
  "data": {},
  "errors": [],
  "timestamp": "2026-06-01T00:00:00Z"
}
```

Empty collections are successful responses, not server errors:

```json
{
  "success": true,
  "message": "No records found",
  "data": [],
  "errors": [],
  "timestamp": "2026-06-01T00:00:00Z"
}
```

Errors use the same envelope with `success: false` and structured `errors[]`.

## Providers

- `GET /api/v1/master/providers`
- `GET /api/v1/master/providers/{providerKey}`
- `PUT /api/v1/master/providers/{providerKey}/credentials`
- `POST /api/v1/master/providers/{providerKey}/test-connection`
- `PATCH /api/v1/master/providers/{providerKey}/status`

Provider responses expose definition, status, supported environments, supported capabilities, and credential status. Saved secrets are never returned.

Credential save request:

```json
{
  "environment": "SANDBOX",
  "secret": "new-secret",
  "webhookUrl": "https://example.test/webhook",
  "active": true
}
```

Curl examples:

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/master/providers

curl -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"environment":"SANDBOX","secret":"sk-new","active":true}' \
  http://localhost:8080/api/v1/master/providers/OPENAI/credentials

curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"environment":"SANDBOX"}' \
  http://localhost:8080/api/v1/master/providers/OPENAI/test-connection
```

## Creative Tools

- `GET /api/v1/master/creative-tools`
- `POST /api/v1/master/creative-tools`
- `GET /api/v1/master/creative-tools/{id}`
- `PUT /api/v1/master/creative-tools/{id}`
- `PATCH /api/v1/master/creative-tools/{id}/status`

`GET` returns an empty list when no tools exist. Seeded tool definitions include campaign creative, post, caption, ad copy, hashtag, product video, and voiceover generators.

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/master/creative-tools
```

## Creative Layers

- `GET /api/v1/master/creative-layers`
- `POST /api/v1/master/creative-layers`
- `GET /api/v1/master/creative-layers/{id}`
- `PUT /api/v1/master/creative-layers/{id}`
- `PATCH /api/v1/master/creative-layers/{id}/status`
- `POST /api/v1/master/creative-layers/reorder`

Default layer definitions are seeded in the master creative pipeline: prompt understanding, brand context, product context, asset analysis, creative generation, quality review, and export preparation.

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/master/creative-layers
```

## Provider Routing Policies

- `GET /api/v1/master/provider-routing-policies`
- `POST /api/v1/master/provider-routing-policies`
- `GET /api/v1/master/provider-routing-policies/{id}`
- `PUT /api/v1/master/provider-routing-policies/{id}`
- `PATCH /api/v1/master/provider-routing-policies/{id}/status`

Active routing policies require an active provider with a configured active credential. Invalid active routing returns HTTP 422 with code `AI-422-01`.

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/master/provider-routing-policies
```

## Monitoring

Monitoring endpoints return `{ "summary": ..., "items": [] }` inside `data`.

- `GET /api/v1/master/ai/provider-health`
- `GET /api/v1/master/ai/layer-analytics?range=30d&provider=&layer=`
- `GET /api/v1/master/ai/cost-usage?range=30d&provider=&workspace=`
- `GET /api/v1/master/ai/failures?range=30d&provider=&layer=&workspace=`

Examples:

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/master/ai/provider-health
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/master/ai/layer-analytics?range=30d"
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/master/ai/failures?range=30d"
```

Empty provider health:

```json
{
  "summary": {
    "totalProviders": 0,
    "healthy": 0,
    "degraded": 0,
    "failed": 0
  },
  "items": []
}
```

## Dashboard

- `GET /api/v1/master/dashboard/summary`

Dashboard sections return zero, null, or empty arrays for modules without data. No-data modules must not fail the whole dashboard request.

## Security Rules

- Provider secrets are accepted only in credential write requests.
- Saved secrets are encrypted through the credential encryption service and are never returned.
- Responses expose credential status and masked status only.
- Metadata sanitization drops secret-like keys before persistence or events.
- Kafka events carry IDs/status/config metadata only, never raw secrets.
