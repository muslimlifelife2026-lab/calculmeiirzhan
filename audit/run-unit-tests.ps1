$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$asciiPath = Join-Path $env:TEMP 'aerocalc-gradle-ascii'

# Gradle's Windows test worker cannot load classes from this project's Cyrillic path.
if (Test-Path -LiteralPath $asciiPath) {
    $existing = (Get-Item -LiteralPath $asciiPath).Target
    if ($existing -ne $projectRoot) {
        throw "Temporary path $asciiPath points to another project: $existing"
    }
} else {
    New-Item -ItemType Junction -Path $asciiPath -Target $projectRoot | Out-Null
}

Push-Location $asciiPath
try {
    & .\gradlew.bat :core-domain:test :core-math-engine:test --offline --no-configuration-cache @args
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
