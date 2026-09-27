$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$env:JAVA_HOME = (Get-ChildItem -LiteralPath "$projectRoot\.tooling\java" -Directory | Select-Object -First 1).FullName
$env:ANDROID_USER_HOME = "$projectRoot\.tooling\android-user"
$sdkRoot = "$projectRoot\.tooling\android-sdk"
$sdkManager = "$sdkRoot\cmdline-tools\cmdline-tools\bin\sdkmanager.bat"
1..20 | ForEach-Object { 'y' } | & $sdkManager "--sdk_root=$sdkRoot" 'ndk;27.0.12077973' 'cmake;3.22.1'
if ($LASTEXITCODE -ne 0) { throw 'Native SDK installation failed' }
