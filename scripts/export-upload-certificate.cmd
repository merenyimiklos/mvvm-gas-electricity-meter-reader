@echo off
setlocal

set "KEYSTORE=meter-reader-release.jks"
set "ALIAS=meter-reader"
set "OUTPUT=upload_certificate.pem"

where keytool >nul 2>&1
if errorlevel 1 (
  echo ERROR: keytool nem talalhato. Telepits JDK 17-et, vagy futtasd olyan CMD-bol, ahol a JDK bin mappa a PATH-ban van.
  exit /b 1
)

if not exist "%KEYSTORE%" (
  echo ERROR: Nem talalom a %KEYSTORE% fajlt ebben a mappaban.
  echo Masold ide a release keystore-t, majd futtasd ujra.
  exit /b 1
)

echo.
echo Meroora Naplo - upload certificate ellenorzes
echo ----------------------------------------------
echo.

echo A keystore tanusitvany ujjlenyomatai:
keytool -list -v ^
  -keystore "%KEYSTORE%" ^
  -alias "%ALIAS%"

if errorlevel 1 exit /b 1

echo.
echo PEM tanusitvany generalasa a Google Play upload key resethez...
keytool -export -rfc ^
  -keystore "%KEYSTORE%" ^
  -alias "%ALIAS%" ^
  -file "%OUTPUT%"

if errorlevel 1 exit /b 1

echo.
echo KESZ:
echo   %OUTPUT%
echo.
echo Ezt a PEM fajlt toltsd fel a Play Console Upload key reset kerelmehez.
echo A jelenlegi GitHub release kulcs SHA-1 ujjlenyomata a fenti listaban lathato.
echo.

endlocal
