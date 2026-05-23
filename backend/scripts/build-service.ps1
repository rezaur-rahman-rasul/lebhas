param(
    [ValidateSet(
        "gateway-service",
        "auth-service",
        "user-service",
        "workspace-service",
        "creative-service",
        "billing-service",
        "notification-service"
    )]
    [string]$Service = "gateway-service"
)

$ErrorActionPreference = "Stop"
$Root = Resolve-Path "$PSScriptRoot\..\.."

& "$Root\mvnw.cmd" -pl "backend/$Service" -am clean package -DskipTests
