@echo off
setlocal

where keytool >nul 2>&1
if errorlevel 1 (
  echo ERROR: keytool nem talalhato. Telepits JDK 17-et, vagy futtasd olyan CMD-bol, ahol a JDK bin mappa a PATH-ban van.
  exit /b 1
)

echo.
echo Meroora Naplo - Android release signing key letrehozasa
echo -------------------------------------------------------
echo A kovetkezo lepesben a keytool jelszavakat es tanusitvanyadatokat ker.
echo A jelszavakat jegyezd fel biztonsagos helyre.
echo.

keytool -genkeypair -v ^
  -keystore meter-reader-release.jks ^
  -alias meter-reader ^
  -keyalg RSA ^
  -keysize 4096 ^
  -validity 10000

if errorlevel 1 exit /b 1

echo.
echo Base64 valtozat keszitese GitHub Secrethez...
certutil -encode meter-reader-release.jks meter-reader-release.b64 >nul

if errorlevel 1 (
  echo ERROR: A base64 fajl nem keszult el.
  exit /b 1
)

echo.
echo KESZ:
echo   meter-reader-release.jks  - ezt mentsd el biztonsagos helyre
echo   meter-reader-release.b64  - ennek TELJES tartalma mehet az ANDROID_KEYSTORE_BASE64 GitHub Secretbe
echo.
echo GitHub Secrets:
echo   ANDROID_KEYSTORE_BASE64
echo   ANDROID_KEYSTORE_PASSWORD
echo   ANDROID_KEY_ALIAS = meter-reader
echo   ANDROID_KEY_PASSWORD
echo.
echo FONTOS: ezeket a fajlokat ne commitold. A .gitignore mar tiltja oket.
echo.

endlocal
