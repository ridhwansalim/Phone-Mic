@echo off
setlocal
set "PROJECT=%~dp0.."
for /f "usebackq delims=" %%i in (`"%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath`) do set "VS=%%i"
if not defined VS exit /b 1
call "%VS%\VC\Auxiliary\Build\vcvars64.bat" >nul
if errorlevel 1 exit /b 1
if not exist "%PROJECT%\.tooling\native-tests" mkdir "%PROJECT%\.tooling\native-tests"
pushd "%PROJECT%\.tooling\native-tests"
set "SRC=%PROJECT%\app\src\main\cpp"
cl /nologo /LD /O2 /DHAVE_CONFIG_H /D_CRT_SECURE_NO_WARNINGS /I"%JAVA_HOME%\include" /I"%JAVA_HOME%\include\win32" /I"%SRC%" /I"%SRC%\speexdsp\include" /I"%SRC%\speexdsp\libspeexdsp" "%SRC%\echo_jni.c" "%SRC%\speexdsp\libspeexdsp\mdf.c" "%SRC%\speexdsp\libspeexdsp\preprocess.c" "%SRC%\speexdsp\libspeexdsp\fftwrap.c" "%SRC%\speexdsp\libspeexdsp\filterbank.c" "%SRC%\speexdsp\libspeexdsp\kiss_fft.c" "%SRC%\speexdsp\libspeexdsp\kiss_fftr.c" /Fe:phonemic_echo.dll
set "RESULT=%ERRORLEVEL%"
popd
exit /b %RESULT%
