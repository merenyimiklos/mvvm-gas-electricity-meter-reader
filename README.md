# Mérőóra Napló

**Aktuális kiadási verzió a következő stable buildben: 1.0.0 (versionCode 20).**

Modern Android alkalmazás otthoni mérőállások rögzítésére, követésére és megosztására.

A cél egyszerű: az áram-, gáz- és vízfogyasztás legyen átlátható, könnyen rögzíthető és egy helyen visszanézhető.

## Fő funkciók

- normál áram, éjszakai áram, gáz és opcionális vízóra kezelése
- a használt mérőtípusok külön ki- és bekapcsolhatók
- gyors havi leolvasás több mérő egyidejű rögzítésével
- egyedi és fotóalapú mérőállás-rögzítés
- OCR-alapú mérőállás-felismerés
- fogyasztási előzmények és havi statisztikák
- költségbecslés egyedi tarifákkal és havi fix díjakkal
- gáz- és vízelszámolás mérőállás vagy havi átalány alapján
- havi fogyasztási célok
- emlékeztetők a hiányzó havi leolvasásokhoz
- JSON biztonsági mentés és visszaállítás
- CSV import/export
- XLSX export
- családi / többkészülékes szinkron
- Firebase Authentication
  - e-mail + jelszó
  - Google-belépés
  - névtelen Firebase-vendégbelépés, később biztonságos összekapcsolás Google-lel vagy új e-mailes fiókkal
  - jelszó-visszaállítás
  - fiók végleges törlése (Google/e-mail esetén újraazonosítással, vendégként külön jelszó nélkül; Beállítások → Fiók törlése)
- magyar és angol felület
- világos, sötét és rendszer témamód
- első indításkor interaktív onboarding
- alkalmazáson belüli verzióellenőrzés
- modern Material 3 felület és animációk

## Mérőtípusok

A felhasználó csak azokat a mérőket kapcsolja be, amelyeket ténylegesen használ:

- **Normál áram** — kWh
- **Éjszakai / vezérelt áram** — kWh
- **Gáz** — m³
- **Víz** — m³

A víz opcionális, így frissítés után a meglévő felhasználóknál nem jelenik meg automatikusan.

## Adatok és költségek

Az alkalmazás a rögzített mérőállások különbségéből számolja a fogyasztást.

A költségbecsléshez külön megadható:

- egységár,
- havi fix díj,
- havi átalány,
- havi fogyasztási cél.

A gáz és a víz esetén választható:

- **Mérőállás alapján**
- **Átalánydíj**

A megjelenített költségek becslések; a tényleges számla szolgáltatótól, tarifától és egyéb díjaktól eltérhet.

## Adattárolás

- **Room / SQLite** — mérőállások
- **Preferences DataStore** — beállítások
- **Firebase Authentication** — felhasználói azonosítás
- **JSON backup** — kézi és automatikus biztonsági mentés

Minden Firebase UID (a névtelen felhasználókat is beleértve) külön Room-adatbázist, DataStore-beállításokat és automatikus mentést használ. Vendégfiók Google-lel vagy új e-mailes fiókkal a Settings / Fiók résznél kapcsolható össze: ugyanaz a Firebase UID marad, tehát a helyi mérőállások nem költöznek. Már létező fiók adatai nem egyesülnek automatikusan; előbb készíts JSON mentést. A vendég kijelentkezése után ugyanaz az anonim fiók nem állítható vissza egyszerű belépéssel; a készülék törlése/újratelepítése a helyi adatokat is eltávolíthatja. A Firebase Identity Platform anonim fiókok 30 napos automatikus tisztítása (ha bekapcsolták) az összekapcsolás nélküli fiókokat érintheti. A felhasználó a Beállítások alján kérheti fiókja végleges törlését; az alkalmazás friss azonosítást kér (jelszó vagy Google), törli a Firebase Authentication-fiókot, majd a hozzá tartozó helyi mérőállásokat, beállításokat és automatikus mentéseket. A megosztott családi mappa és a kézzel exportált fájlok nem törlődnek automatikusan. Az eltávolíthatatlan külső mentésekre a felület figyelmeztet.

A helyi adatok frissítéskor megmaradnak, amennyiben az alkalmazás azonos csomagnévvel és megfelelő aláírással frissül.

## Google Play fióktörlési megfelelés

A Google Play a fiók törléséhez az alkalmazáson belüli menüpont **mellett külön nyilvános webes kérelmezési útvonalat is kér**, amely az alkalmazás újratelepítése nélkül is működik. A Play Console Data safety / Account deletion mezőjében egy üzemelő űrlap vagy támogatási e-mailt megnyitó weboldal URL-jét kell megadni. A nyilvános weboldalt és az ott beérkező kérelmek feldolgozását **külön kell beállítani**; önmagában ez az Android-funkció nem helyettesíti.

## Verziózás

A következő stable kiadástól a felhasználónak megjelenített verzió **1.0.0**. Hibajavítás: **1.0.1**, új funkció: **1.1.0**, jelentősebb változás: **2.0.0**. Az Android `versionCode` ettől független, minden Play feltöltéshez egyedi és növekvő; az 1.0.0 kiadás alapértelmezett kódja **20**. A `Create App Release` workflow beállított alapértékei a `version_name=1.0.0`, `version_code=20` és `channel=stable`.

## Technológia

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Room
- DataStore
- Coroutines / Flow
- WorkManager
- Firebase Authentication
- Android Credential Manager
- ML Kit Text Recognition
- Lottie
- R8 optimalizálás és resource shrinking

## Projektstruktúra

```text
Compose UI
   ↓
ViewModel
   ├── MeterRepository → Room
   ├── SettingsRepository → DataStore
   ├── UsageCalculator
   ├── AnalyticsCalculator
   ├── OcrMeterReader
   ├── BackupManager
   ├── CSV / XLSX export
   ├── Reminder Worker
   └── Shared / family sync
```

## Build

JDK 17 szükséges.

### Windows

```bat
gradlew.bat testDebugUnitTest assembleDebug
```

### Linux / macOS

```bash
./gradlew testDebugUnitTest assembleDebug
```

A CI unit teszteket, debug buildet és optimalizált release buildet is ellenőriz.

## Firebase

A Firebase Authentication beállításának részletes leírása:

```
FIREBASE_SETUP.md
```

Android package:

```
hu.merenyimiklos.meterreader
```

## Jogi oldalak

A Google Play számára használt publikus jogi oldalak a `docs/` mappában találhatók:

- `docs/privacy.html` — Adatvédelmi tájékoztató
- `docs/terms.html` — Felhasználási feltételek / ÁSZF
- `docs/account-deletion.html` — Fiók és adatok törlése

GitHub Pages engedélyezése után ezek nyilvános HTTPS URL-en érhetők el.

## Megjegyzés

Az OCR és a költségbecslés segédfunkció. A felismert mérőállást mentés előtt mindig érdemes ellenőrizni.

---

**Mérőóra Napló** — egyszerűbb leolvasás, átláthatóbb fogyasztás.
