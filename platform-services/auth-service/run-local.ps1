$envFile = Join-Path $PSScriptRoot "..\..\.env"

if (-not (Test-Path $envFile)) {
    Write-Error "Root .env file not found at: $envFile"
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()

    if ($line -and -not $line.StartsWith("#")) {
        $key, $value = $line -split "=", 2

        if ($key -and $value) {
            [Environment]::SetEnvironmentVariable(
                $key.Trim(),
                $value.Trim(),
                "Process"
            )
        }
    }
}

$env:JAVA_TOOL_OPTIONS = "-Duser.timezone=Asia/Kolkata"

.\mvnw.cmd spring-boot:run