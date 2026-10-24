# Mérőóra Napló

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
  - jelszó-visszaállítás
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

A helyi adatok frissítéskor megmaradnak, amennyiben az alkalmazás azonos csomagnévvel és megfelelő aláírással frissül.

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

## Megjegyzés

Az OCR és a költségbecslés segédfunkció. A felismert mérőállást mentés előtt mindig érdemes ellenőrizni.

---

**Mérőóra Napló** — egyszerűbb leolvasás, átláthatóbb fogyasztás.
