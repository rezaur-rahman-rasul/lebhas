# Master Dashboard API

`GET /api/v1/master/dashboard/summary`

Returns `200 OK` for empty systems. Empty usage, missing provider health, and missing activity are represented with zero, `null`, empty arrays, and `hasData: false`; they are not server errors.

Requires `MASTER`.
