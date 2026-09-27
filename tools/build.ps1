param([switch]$Lint)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$localJava = Get-ChildItem -LiteralPath "$projectRoot\.tooling\java" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if ($localJava) { $env:JAVA_HOME = $localJava.FullName }
if (-not $env:JAVA_HOME) { throw 'Install JDK 17 and set JAVA_HOME, or provide .tooling/java/<jdk>.' }
$env:ANDROID_HOME = "$projectRoot\.tooling\android-sdk"
$env:GRADLE_USER_HOME = "$projectRoot\.tooling\gradle-home"
$env:ANDROID_USER_HOME = "$projectRoot\.tooling\android-user"
$gradle = "$projectRoot\.tooling\gradle-8.11.1\bin\gradle.bat"
if (-not (Test-Path -LiteralPath $gradle)) { throw 'Extract Gradle 8.11.1 into .tooling or build through Android Studio.' }
New-Item -ItemType Directory -Force "$projectRoot\.tooling\test-classes" | Out-Null
& "$env:JAVA_HOME\bin\javac.exe" -d .tooling/test-classes app/src/main/java/com/phonemic/app/AudioSettings.java app/src/main/java/com/phonemic/app/FeedbackGuard.java app/src/main/java/com/phonemic/app/AudioProcessor.java app/src/main/java/com/phonemic/app/SessionState.java tests/AudioProcessorTest.java tests/SessionStateTest.java
if ($LASTEXITCODE -ne 0) { throw 'DSP test compilation failed' }
& "$env:JAVA_HOME\bin\java.exe" -cp .tooling/test-classes com.phonemic.app.AudioProcessorTest
if ($LASTEXITCODE -ne 0) { throw 'DSP tests failed' }
& "$env:JAVA_HOME\bin\java.exe" -cp .tooling/test-classes com.phonemic.app.SessionStateTest
if ($LASTEXITCODE -ne 0) { throw 'Session state tests failed' }
$tasks = @('wrapper', 'assembleDebug')
if ($Lint) { $tasks += 'lintDebug' }
& $gradle @tasks --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Android build failed' }
New-Item -ItemType Directory -Force "$projectRoot\dist" | Out-Null
Copy-Item -LiteralPath "$projectRoot\app\build\outputs\apk\debug\app-debug.apk" -Destination "$projectRoot\dist\PhoneMic-debug.apk"
Copy-Item -LiteralPath "$projectRoot\app\build\outputs\apk\debug\app-debug.apk" -Destination "$projectRoot\dist\PhoneMic-v1.4-debug.apk"
