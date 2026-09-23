# Mérőóra Napló

Jetpack Compose alapú Android alkalmazás havi áram- és gázmérőállások rögzítésére, fogyasztási kimutatásokkal és költségbecsléssel.

## Funkciók

- külön **normál**, **éjszakai/vezérelt** áram- és gázmérőállás kezelése
- **Gyors havi leolvasás**: mindhárom mérő egy képernyőn, egyetlen mentéssel
- Material 3 **DatePicker**, előző mérőállás, élő fogyasztás- és költségbecslés
- fotózás vagy galériakép alapján ML Kit OCR-es mérőállás-felismerés
- mérőfotók visszanézése a Naplóban
- részletes mérési adatlap: előző állás, eltelt napok, napi átlag, becsült költség
- duplikációvédelem és szokatlan mérőugrások figyelmeztetése
- havi és éves fogyasztási összesítések
- külön **Statisztika** nézet 12 havi diagramokkal
- előző havi és előző évi összehasonlítás
- 3 havi átlag, napi átlag, minimum/maximum és automatikus kiugrásjelzés
- havi fogyasztási célok és havi előrejelzés
- **magyar lakossági rezsikeret-becslés**
  - 2523 kWh/év külön a normál és külön az éjszakai/vezérelt mérőre
  - gáz 63 645 MJ/év keret és szezonális havi MVM-görbe
  - állítható gáz-fűtőérték a m³-becsléshez
- becsült havi költségdiagram
- minden hónap **23-ától napi leolvasási értesítés**, amíg mindhárom e havi mérés nincs meg
- az értesítésből közvetlenül megnyitható a Rögzítés képernyő
- kezdőképernyős Android widget a három legutóbbi mérőállással
- appikon hosszú nyomására gyors **Új mérőállás** parancs
- állítható egységárak, havi fix díjak és gáz-átalány
- mérőállások szerkesztése, törlése és típus szerinti szűrése
- valódi **.xlsx** export Excelhez
- **CSV export és import**
- kézi **JSON biztonsági mentés és visszaállítás**
- minden adatváltozás után automatikus helyi JSON backup
- opcionálisan kiválasztható külső/Drive-kompatibilis SAF mappa az automatikus backuphoz
- Room adatbázis a mérőállásokhoz
- Preferences DataStore a beállításokhoz
- automatikus migráció a korábbi SharedPreferences alapú verzióból
- Material 3 + Jetpack Compose
- MVVM felépítés
- GitHub Actions unit teszt + debug APK artifact

## Adattárolás

A mérőállások Room/SQLite adatbázisban vannak, a tarifák, célok, emlékeztető és mentési beállítások pedig DataStore-ban.

Az alkalmazás az első induláskor megpróbálja automatikusan áthozni a 0.1.x verzió SharedPreferences + JSON adatait.

A **Beállítások → Adatkezelés** részen készíthető JSON mentés, CSV export/import és kiválasztható külső automatikus mentési mappa. Az app minden adatváltozás után frissít egy belső `auto-backup.json` fájlt, és ha külső mappa van beállítva, oda is tükrözi azt. A fotófájlokat a JSON/CSV mentés nem ágyazza be.

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
   ├── HungarianEnergyLimits
   ├── OcrMeterReader
   ├── XlsxExporter / CsvManager
   ├── BackupManager
   ├── ReadingReminderWorker
   └── MeterWidgetProvider
```

## Build

JDK 17 szükséges.

```bash
./gradlew testDebugUnitTest assembleDebug
```

A GitHub Actions minden `main` pushnál és PR-nél lefuttatja a unit teszteket, elkészíti a debug APK-t, majd artifactként feltölti.

## Csomagnév

`hu.merenyimiklos.meterreader`
