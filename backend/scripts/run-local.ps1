param(
    [ValidateSet("up", "down", "restart", "logs", "ps")]
    [string]$Action = "up",

    [ValidateSet(
        "gateway-service",
        "auth-service",
        "workspace-service",
        "creative-service",
        "postgres",
        "redis"
    )]
    [string]$Service = "gateway-service"
)

$ErrorActionPreference = "Stop"
$Root = Resolve-Path "$PSScriptRoot\..\.."
$ComposeFile = Join-Path $Root "backend\docker\docker-compose.yml"

switch ($Action) {
    "up" {
        docker compose -f $ComposeFile up --build -d
    }
    "down" {
        docker compose -f $ComposeFile down
    }
    "restart" {
        docker compose -f $ComposeFile down
        docker compose -f $ComposeFile up --build -d
    }
    "logs" {
        docker compose -f $ComposeFile logs -f $Service
    }
    "ps" {
        docker compose -f $ComposeFile ps
    }
}
