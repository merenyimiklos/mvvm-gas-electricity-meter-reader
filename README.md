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
- szabadon beállítható havi leolvasási emlékeztető **nap + időpont** alapján
- a kiválasztott naptól napi értesítés, amíg mindhárom e havi mérés nincs meg
- app-on-belüli stable verzióellenőrzés és frissítési figyelmeztetés
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


## GitHub kiadások

A repository a **Create App Release** GitHub Actionből tud zárt béta vagy stable kiadást készíteni.

### Kiadás egy korábbi sikeres Actionből

Az **Actions → Create App Release → Run workflow** képernyőn megadható a `source_run_id`.

Ez annak a sikeres GitHub Actions futásnak az azonosítója, amelyből kiadást szeretnél készíteni. Például egy ilyen URL-ben:

`.../actions/runs/40019995720`

a run ID:

`40019995720`

A release workflow ellenőrzi, hogy a kiválasztott Action **completed + success** állapotú-e, kiveszi belőle a pontos commit SHA-t, majd ugyanabból a commitból újrafuttatja a teszteket és elkészíti a release buildet.

Ha nem adsz meg `source_run_id`-t, a `source_ref` mezőben branch, tag vagy commit SHA is megadható.

### Zárt béta

Válaszd:

- `channel: beta`
- `source_run_id`: opcionálisan egy sikeres Action run ID
- `source_ref`: alapból `main`
- `version_name`: az alapverzió, például `0.5.0`
- `version_code`: mindig növekvő egész szám
- opcionális release notes

A workflow unit teszteket futtat, release-signed APK-t készít, SHA-256 checksumot generál, majd **Draft + Prerelease** GitHub Release-t hoz létre. A béta APK verzióneve automatikusan például `0.5.0-beta.123` lesz.

A repository publikus, de a Draft release nem jelenik meg nyilvánosan; draft release-eket csak push jogosultsággal rendelkező felhasználók látják.

### Signing key egyszeri beállítása

A béta és a stable ugyanazt az állandó signing key-t használja, így a béta verziók és a későbbi stable verzió egymásra telepíthetők.

Windows alatt CMD-ből futtasd:

`scripts\\create-release-keystore.cmd`

Ez létrehozza:

- `meter-reader-release.jks`
- `meter-reader-release.b64`

A JKS fájlt mentsd el biztonságos helyre. A repository `.gitignore` fájlja kizárja a signing fájlokat.

A GitHub repository **Settings → Secrets and variables → Actions** részében hozd létre:

- `ANDROID_KEYSTORE_BASE64` – a `meter-reader-release.b64` teljes tartalma
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS` – alapból `meter-reader`
- `ANDROID_KEY_PASSWORD`

A signing key és a jelszavak soha ne kerüljenek commitba.

### Stable

Ha a béta rendben van, indítsd újra a **Create App Release** workflow-t:

- ugyanazzal a forrás Actionnel vagy commit SHA-val
- `channel: stable`
- a kívánt végleges `version_name` és `version_code` értékekkel

A stable workflow signed release APK-t készít, `v<version>` taget hoz létre és publikus **Latest GitHub Release**-ként teszi közzé.

A Gradle build CI-ből felülírható az `APP_VERSION_NAME` és `APP_VERSION_CODE` környezeti változókkal, ezért a béta/stable kiadáshoz nem kell kézzel átírni a repository verziószámát.


## Frissítés és adatmegőrzés

Azonos `applicationId` és azonos release signing key esetén az APK frissítése megőrzi a Room adatbázist, a DataStore beállításokat és az app belső adatait.

### Első átállás debug APK-ról signed bétára

A GitHub Actions debug APK és a saját release key-jel aláírt béta eltérő aláírást használ. Emiatt az első signed béta nem telepíthető közvetlenül a korábbi debug APK fölé.

Az első átállás menete:

1. a régi appban készíts JSON biztonsági mentést;
2. töröld a debug verziót;
3. telepítsd a release-signed bétát;
4. állítsd vissza a JSON mentést.

Ezután a további **beta → beta → stable** frissítések ugyanazzal a signing key-jel készülnek, ezért rátelepíthetők és az adatok megmaradnak.

## Beépített frissítésellenőrzés

Az app induláskor ellenőrzi a repository legfrissebb **publikus stable GitHub Release** kiadását. Ha újabb verzió érhető el, Material 3 frissítési ablak jelenik meg, amely közvetlenül az APK letöltésére visz.

A zárt, Draft béta release-eket az app nem kérdezi le, mert azok GitHub-hitelesítést igényelnének; hozzáférési tokent nem tárolunk az APK-ban.
