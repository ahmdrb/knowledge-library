# Feature inventory and migration status

Audit source: repository ZIP supplied by the user (`knowledge-library-android-main.zip`).

| Feature evidenced in source/README | Original implementation | Native-preview status | Notes / verification |
|---|---|---|---|
| Import UTF-8 `.txt` outline | Android `OpenDocument` bridge + HTML/JS parser | Implemented with Storage Access Framework | Runtime behavior not device-tested |
| First line is package title | JavaScript outline parser | Implemented in Kotlin parser | Four spaces per hierarchy level; numbered labels stripped |
| Hierarchical concept tree | HTML/JS rendering and local JSON state | Basic native tree display | Deep mutation/recomposition requires further QA |
| Search package/concept titles | HTML/JS search | Basic package and nested concept title filtering | Original scope/package ranking filters not yet ported |
| Multiple local packages | WebView localStorage | Native SharedPreferences JSON | Not a Room database; migration from WebView localStorage not automatic |
| Replace package by matching title | Import logic | TXT import replaces title match | Backup import merges/replaces by title |
| Delete package | HTML UI | Implemented | No confirmation dialog yet |
| Star concepts/favorites | JavaScript state | Star toggling and favorite-only package filtering | Full favorites screen/filters not yet ported |
| Expand/collapse branches | HTML tree UI | Basic expand/collapse | UI regression tests pending |
| Edit complete outline | HTML editor | Basic native text editor dialog | Re-parses as a package outline |
| Add package / top-level concept | UI workflow | Implemented | Nested add/edit and full original action set not yet ported |
| Export JSON backup | Native document creation bridge | Emits original `knowledge-library-backup` format, version 1 | Source schema matched by inspection; runtime round-trip not tested |
| Import JSON backup | JavaScript validates `format`, `version`, package titles and nodes | Validates the original format and parses all packages before merge | Runtime round-trip and malformed-input tests pending |
| Bulk select/move/transfer/delete | Present in HTML/JS source | Not yet ported | Required parity work |
| Parent-path navigation / concept detail | README and HTML/JS | Not yet ported as dedicated screens | Required parity work |
| Offline operation / no network permission | Manifest and README | Preserved; no network permission added | No backend identified in supplied source |
| Hide/show app buttons | HTML/CSS UI | Not yet ported | Lower priority UI preference |

## Confirmed project configuration

- Application ID and namespace: `com.knowledgelibrary`
- Kotlin Android application
- Android Gradle Plugin 8.7.3; Kotlin 2.0.21
- Compile/target SDK 35; minimum SDK 23
- Java/JVM target 17
- Original launcher implementation: `MainActivity.kt` hosting `assets/index.html` in a WebView
- Original persisted library data: WebView localStorage according to README and JavaScript source
- Original native integrations: document picker for text import and document creation for JSON export
- No network permission or backend integration found in supplied manifest/source

Status labels: “Implemented” means source code exists in this preview, not that compilation or device operation has been verified.
