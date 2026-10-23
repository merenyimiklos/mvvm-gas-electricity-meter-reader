# Firebase setup – Mérőóra Napló

A 0.9.0 verziótól az alkalmazás Firebase Authenticationt használ.

## 1. Firebase projekt

1. Nyisd meg a Firebase Console-t.
2. Hozz létre egy projektet.
3. Adj hozzá Android alkalmazást.

Android package name:

```
hu.merenyimiklos.meterreader
```

## 2. SHA tanúsítványok

A Google-belépéshez add hozzá a Firebase Android apphoz a szükséges SHA-1 ujjlenyomatokat.

### Helyi debug build

Android Studioból vagy terminálból:

```
./gradlew signingReport
```

Windows alatt:

```
gradlew.bat signingReport
```

A `debug` variáns SHA-1 értékét add hozzá Firebase-ben.

### Google Play build

A Google Play Consoleban az **Alkalmazások aláírása / App signing** oldalon az
**alkalmazás-aláíró kulcs tanúsítványának SHA-1** értékét add hozzá Firebase-ben.

Ez nem ugyanaz, mint az upload key.

Ha a GitHubról letölthető, közvetlenül telepített release APK-ban is szeretnéd a
Google-belépést használni, akkor a GitHub Actions release keystore SHA-1 értékét
is add hozzá.

## 3. Authentication providerek

Firebase Console:

**Authentication → Sign-in method**

Engedélyezd:

- Email/Password
- Google

A Google provider engedélyezése után töltsd le újra a
`google-services.json` fájlt, hogy az OAuth kliens adatai is benne legyenek.

## 4. google-services.json

Két lehetőség van.

### Helyi fejlesztés

Másold ide:

```
app/google-services.json
```

A Gradle automatikusan érzékeli, és csak ekkor kapcsolja be a
`com.google.gms.google-services` plugint.

### GitHub Actions

Repo:

**Settings → Secrets and variables → Actions → New repository secret**

Név:

```
FIREBASE_GOOGLE_SERVICES_JSON
```

Érték:

a letöltött `google-services.json` teljes tartalma.

A normál debug CI Firebase config nélkül is fordul.
A release workflow viszont 0.9.0-tól nem enged kiadást készíteni Firebase config nélkül.

## 5. Jelenlegi auth funkciók

- Email + jelszó belépés
- Regisztráció
- Elfelejtett jelszó / reset email
- Google Sign-In Credential Managerrel
- Firebase kijelentkezés
- Animált Lottie auth képernyő
- Magyar / angol auth UI
- Debug-only offline fejlesztői mód Firebase config hiányakor

## 6. Következő lépés – Firebase családi csoport

A jelenlegi fájlalapú Közös otthon funkció később Firestore alapú családi csoporttá alakítható.

Javasolt struktúra:

```
users/{uid}
homes/{homeId}
homes/{homeId}/members/{uid}
homes/{homeId}/readings/{readingId}
homes/{homeId}/invites/{inviteId}
```

A hozzáférést Firestore Security Rules szabályozza majd úgy, hogy egy otthon
adatait kizárólag annak tagjai olvashassák és módosíthassák.
