# Master Usage API

Endpoints:

- `GET /api/v1/master/usage/overview`
- `GET /api/v1/master/usage/workspaces`
- `GET /api/v1/master/usage/ai-costs`
- `GET /api/v1/master/usage/plan-utilization`

No usage records return `200 OK` with empty `items` or empty arrays. List endpoints include pagination metadata where records can grow.
