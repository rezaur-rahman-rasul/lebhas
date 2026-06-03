# Docker Setup

Docker must be installed on Windows and available from PowerShell before the backend stack can run.

Check Docker:

```powershell
docker --version
docker compose version
```

Create the local environment file:

```powershell
Copy-Item .env.example .env
```

Edit `.env` and set real Cloudflare R2 values:

```env
STORAGE_PROVIDER=R2
R2_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
R2_BUCKET=<bucket-name>
R2_ACCESS_KEY=<r2-access-key-id>
R2_SECRET_KEY=<r2-secret-access-key>
R2_REGION=auto
R2_PUBLIC_BASE_URL=
R2_PATH_STYLE_ACCESS_ENABLED=true
```

Start the stack:

```powershell
docker compose up -d --build
```

Verify `creative-service` received the storage variables without printing secrets:

```powershell
docker exec creative-saas-creative printenv STORAGE_PROVIDER R2_ENDPOINT R2_BUCKET R2_REGION
```

If signed asset uploads fail, inspect the service log:

```powershell
docker logs creative-saas-creative --tail 100
```
