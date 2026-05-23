ALTER ROLE postgres WITH PASSWORD 'admin';

SELECT 'CREATE DATABASE creative_saas OWNER postgres'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'creative_saas')\gexec

\connect creative_saas

CREATE SCHEMA IF NOT EXISTS platform AUTHORIZATION postgres;
GRANT ALL PRIVILEGES ON DATABASE creative_saas TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA platform TO postgres;
