# Mérőóra Napló

Jetpack Compose alapú Android alkalmazás havi áram- és gázmérőállások rögzítésére, fogyasztási kimutatásokkal és költségbecsléssel.

## Funkciók

- külön **normál**, **éjszakai** áram- és gázmérőállás rögzítése
- fotózás vagy galériakép alapján ML Kit OCR-es mérőállás-felismerés
- mentés előtti OCR-ellenőrzés
- havi fogyasztás számítása kumulatív mérőállások különbségéből
- havi és éves összesítések
- külön **Statisztika** nézet 12 havi fogyasztási diagramokkal
- normál / éjszakai / összes áram / gáz trendek
- előző havi változás, 3 havi átlag és legutóbbi napi átlag
- minimum/maximum hónap és automatikus fogyasztási kiugrásjelzés
- becsült havi költségdiagram
- becsült fizetendő összeg
- külön gáz **átalánydíjas** és **mérőállás alapú** mód
- külön állítható normál és éjszakai áram egységárak és havi fix díjak
- mérőállások utólagos szerkesztése és törlése
- normál / éjszakai / gáz előzmény-szűrés
- valódi **.xlsx** export Excelhez
- **JSON biztonsági mentés és visszaállítás**
- Room adatbázis a mérőállásokhoz
- Preferences DataStore a beállításokhoz
- automatikus migráció a korábbi SharedPreferences alapú verzióból
- saját adaptív Android launcher ikon
- Material 3 + Jetpack Compose
- MVVM felépítés
- GitHub Actions debug APK artifact

## Adattárolás

A 0.2.0 verziótól a mérőállások Room/SQLite adatbázisban vannak, a díjbeállítások pedig DataStore-ban.

Az alkalmazás az első induláskor megpróbálja automatikusan áthozni a 0.1.x verzió SharedPreferences + JSON adatait.

A **Beállítások → Adatkezelés** részen készíthető JSON mentés. A biztonsági mentés a mérőállásokat és a díjbeállításokat tartalmazza; a fotófájlokat nem ágyazza be.

## Fontos

A költségszámítás becslés. A tényleges számlázás szolgáltatótól, tarifacsomagtól, kedvezményes/versenypiaci sávoktól, korrekciós tényezőktől és egyéb díjaktól függhet. Emiatt az alkalmazás nem éget be aktuális szolgáltatói árakat: ezeket a **Beállítások** képernyőn kell megadni.

Az OCR segédfunkció. A felismert mérőállás mentés előtt szerkeszthető és ellenőrizendő.

## Technológia

- Kotlin 2.3
- Jetpack Compose
- Material 3
- Navigation Compose
- Room 2.8
- Preferences DataStore
- Kotlin Coroutines / Flow
- ML Kit Text Recognition
- saját minimális XLSX generátor
- JUnit unit tesztek

## Architektúra

```
Compose UI
   ↓
MeterViewModel
   ├── MeterRepository
   │     ↓
   │   Room / SQLite
   │
   ├── SettingsRepository
   │     ↓
   │   Preferences DataStore
   │
   ├── UsageCalculator
   ├── AnalyticsCalculator
   ├── OcrMeterReader
   ├── XlsxExporter
   └── BackupManager
```

## Build

JDK 17 szükséges.

```bash
./gradlew testDebugUnitTest assembleDebug
```

A GitHub Actions minden `main` pushnál és PR-nél lefuttatja a unit teszteket, elkészíti a debug APK-t, majd artifactként feltölti.

## Csomagnév

`hu.merenyimiklos.meterreader`
