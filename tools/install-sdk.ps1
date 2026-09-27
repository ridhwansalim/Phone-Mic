$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$env:JAVA_HOME = (Get-ChildItem -LiteralPath "$projectRoot\.tooling\java" -Directory | Select-Object -First 1).FullName
$env:ANDROID_USER_HOME = "$projectRoot\.tooling\android-user"
$sdkRoot = "$projectRoot\.tooling\android-sdk"
$sdkManager = "$sdkRoot\cmdline-tools\cmdline-tools\bin\sdkmanager.bat"
1..20 | ForEach-Object { 'y' } | & $sdkManager "--sdk_root=$sdkRoot" 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'
if ($LASTEXITCODE -ne 0) { throw 'Android SDK installation failed' }
