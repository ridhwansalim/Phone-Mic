$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$env:JAVA_HOME = (Get-ChildItem -LiteralPath "$projectRoot\.tooling\java" -Directory | Select-Object -First 1).FullName
& "$PSScriptRoot\compile-native-tests.cmd"
if ($LASTEXITCODE -ne 0) { throw 'Host native library compilation failed' }
& "$env:JAVA_HOME\bin\javac.exe" -d .tooling/test-classes experiments/speaker-echo/java/NativeEcho.java experiments/speaker-echo/java/EchoDelayEstimator.java experiments/speaker-echo/java/SpeakerEcho.java experiments/speaker-echo/tests/SpeakerEchoTest.java
if ($LASTEXITCODE -ne 0) { throw 'Echo test compilation failed' }
& "$env:JAVA_HOME\bin\java.exe" '-Djava.library.path=.tooling/native-tests' -cp .tooling/test-classes com.phonemic.app.SpeakerEchoTest
if ($LASTEXITCODE -ne 0) { throw 'Echo cancellation tests failed' }
