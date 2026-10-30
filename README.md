# NothingPacker ●

**Offline APK ⇄ IPA toolkit for Android** – part of the fan-made Nothing ecosystem.
Analyse, extract and re-package Android and iOS app packages directly on your phone.
OLED black, dot-matrix typography, a red accent – and not a single byte leaves the device.

> 🇩🇪 Deutsche Beschreibung weiter unten.

## Features

| | APK (Android) | IPA (iOS) |
|---|---|---|
| **Metadata** | Binary `AndroidManifest.xml` decoded to readable XML; label & icon resolved via `resources.arsc` | `Info.plist` (binary or XML) decoded |
| **Details** | package, version, min/target SDK, permissions, activities/services/receivers/providers, ABIs, signature scheme (v1 / v2+) | bundle id, version, minimum iOS, usage descriptions, URL schemes, app extensions, frameworks, CPU arch, FairPlay encryption |
| **Files** | every entry, grouped: code, native libs, resources, assets, metadata, signature | executable, frameworks, resources (`.car`, `.nib`, `.lproj`), assets, signature |
| **Icon** | highest-density PNG/WebP | Apple CgBI PNGs converted back to standard PNG |
| **Extract** | choose categories → folder of your choice; manifest as readable XML + text report | same, `Info.plist` as XML |
| **Re-package** | → **IPA structure**: `Payload/App.app/` with generated `Info.plist`, icon, assets, resources | → **APK structure**: generated `AndroidManifest.xml`, `strings.xml`, launcher icon, assets |

Permissions are mapped between platforms where a counterpart exists
(e.g. `android.permission.CAMERA` ⇄ `NSCameraUsageDescription`).

### What "conversion" can and cannot do

Android apps run DEX bytecode on ART, iOS apps run signed arm64 Mach-O binaries against Apple
frameworks. **Program code cannot be translated between the two**, and no tool can turn an APK into
an installable IPA (or the other way round). NothingPacker therefore performs a *structural*
conversion: it re-arranges metadata, icon, assets and resources into the layout of the other
platform as a starting point for a native rebuild. The archive contains a README that says so, and
never contains code from the source package.

Only analyse and convert apps you own or are allowed to modify.

## Privacy by design

- 🚫 **No permissions at all** – no `INTERNET`, no storage access. Both are explicitly stripped from the
  merged manifest (`tools:node="remove"`), and CI fails if `INTERNET` ever appears.
- 📂 **System file picker only** – packages are opened and results saved via the Storage Access Framework,
  exactly where you choose.
- 🧹 **Private workspace** – opened packages are copied to the app cache for random access and deleted when
  you leave the analysis, on next start, or via *Settings → Clear now*.
- 💾 **No backups** of app data, no analytics, no crash reporting.
- 🛡️ **Hardened parsers** – pure-Kotlin AXML, ARSC, plist, PNG and Mach-O readers with bounds checks,
  zip-bomb limits, path sanitising (no zip-slip) and no XML entity/DTD resolution (no XXE).

## Project structure

```
NothingPacker/
├── build.gradle.kts · settings.gradle.kts · gradle/libs.versions.toml
├── .github/workflows/android.yml              # tests, APK build, INTERNET-permission check
└── app/src/
    ├── main/
    │   ├── AndroidManifest.xml                # no permissions
    │   ├── java/com/real3rr0r303yt/nothingpacker/
    │   │   ├── MainActivity.kt
    │   │   ├── core/                          # pure Kotlin, JVM-testable
    │   │   │   ├── axml/      binary XML decoder (AndroidManifest.xml)
    │   │   │   ├── arsc/      resources.arsc reader (label + icon)
    │   │   │   ├── plist/     binary + XML property lists
    │   │   │   ├── image/     CgBI → PNG
    │   │   │   ├── macho/     Mach-O / fat header (arch, FairPlay)
    │   │   │   ├── analysis/  APK + IPA analysers
    │   │   │   └── convert/   extractor, structure converter, permission map
    │   │   ├── data/          workspace, SAF folder sink, icon scaling
    │   │   └── ui/            theme · components (dot matrix) · home · analysis · settings
    │   └── res/ (values, values-de, xml)
    └── test/                                  # parser, analyser and converter tests
```

## Build

Requirements: JDK 17+, Android SDK 35.

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/
```

Min SDK 26 (Android 8.0), target SDK 35. Kotlin 2.1, Jetpack Compose (Material 3). No Room, no network
libraries, no native code.

## Support

If NothingPacker helps you: [☕ Buy Me a Coffee](https://buymeacoffee.com/3rr0r303YT)

---

## 🇩🇪 Deutsch

**NothingPacker** analysiert, entpackt und packt APK- und IPA-Dateien um – komplett offline auf dem
Smartphone, im Nothing-OS-Look.

- **Analyse:** Manifest bzw. Info.plist lesbar, Berechtigungen, Komponenten, Architekturen, Signatur, alle Dateien.
- **Entpacken:** Assets, Ressourcen, Metadaten und Icon in einen frei wählbaren Ordner.
- **Umpacken (APK ⇄ IPA):** Struktur-Archiv für die jeweils andere Plattform mit erzeugten Metadaten,
  Icon, Assets und Ressourcen. Programmcode lässt sich technisch nicht zwischen Android und iOS übersetzen –
  das Ergebnis ist die Basis für einen nativen Neubau, keine installierbare App.
- **Privatsphäre:** keine Internet-, keine Speicher-Berechtigung; Dateizugriff nur über die System-Dateiauswahl.
- **Sprachen:** Deutsch & Englisch – folgt dem System oder wird in den Einstellungen umgestellt.

Analysiere und konvertiere nur Apps, die dir gehören oder die du verändern darfst.

## License

Copyright © 2026 **Real3rr0r303YT**

This program is free software: you can redistribute it and/or modify it under the terms of the
**GNU General Public License v3.0** as published by the Free Software Foundation, either version 3
of the License, or (at your option) any later version. See [LICENSE](LICENSE).

NothingPacker is an unofficial fan project and is not affiliated with Nothing Technology Limited,
Google LLC or Apple Inc. Android is a trademark of Google LLC; iOS is a trademark of Apple Inc.

<p align="center"><b>Made by <a href="https://github.com/Real3rr0r303YT">Real3rr0r303YT</a></b></p>
