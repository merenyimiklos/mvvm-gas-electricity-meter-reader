# Mérőóra Napló

Jetpack Compose alapú Android alkalmazás havi áram- és gázmérőállások rögzítésére.

## Funkciók

- áram- és gázmérőállás kézi rögzítése
- fotózás vagy galériakép alapján OCR-es mérőállás-felismerés
- havi fogyasztás számítása kumulatív mérőállások különbségéből
- havi fogyasztási diagramok
- becsült fizetendő összeg
- külön gáz **átalánydíjas** és **mérőállás alapú** mód
- állítható egységárak és havi fix díjak
- valódi **.xlsx** export Excelhez
- helyi adattárolás, backend nélkül
- Material 3 + Jetpack Compose
- MVVM felépítés

## Fontos

A költségszámítás becslés. A tényleges számlázás szolgáltatótól, tarifacsomagtól, kedvezményes/versenypiaci sávoktól, korrekciós tényezőktől és egyéb díjaktól függhet. Emiatt az alkalmazás nem éget be aktuális szolgáltatói árakat: ezeket a **Beállítások** képernyőn kell megadni.

Az OCR szintén segédfunkció. A felismert mérőállás mentés előtt szerkeszthető és ellenőrizendő.

## Technológia

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Kotlin Coroutines / Flow
- ML Kit Text Recognition
- lokális SharedPreferences + JSON repository
- saját minimális XLSX generátor

## Architektúra

```
UI (Compose)
   ↓
MeterViewModel
   ↓
LocalMeterRepository
   ↓
SharedPreferences / JSON

MeterViewModel
   ├── UsageCalculator
   ├── OcrMeterReader
   └── XlsxExporter
```

A repository jelenlegi első verziója szándékosan egyszerű, lokális és egyfelhasználós. Ha később Play Áruházba kerül, következő PR-ekben érdemes külön migrálni Roomra, hozzáadni backup/import funkciót, több tarifás számítást, teszteket és Play-ready adatvédelmi dokumentációt.

## Build

JDK 17 szükséges.

```bash
./gradlew assembleDebug
```

## Csomagnév

`hu.merenyimiklos.meterreader`
