$ErrorActionPreference = "Stop"
$Repo = Split-Path -Parent $PSScriptRoot
Set-Location $Repo

.\gradlew.bat test
.\gradlew.bat clean build
.\gradlew.bat build --warning-mode all
git diff --check
