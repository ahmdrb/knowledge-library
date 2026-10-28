# Changelog

## 2.0-native-preview (2026-10-28)

- Replaced the launcher WebView entry point with a Jetpack Compose native preview.
- Added native text outline import through Android's Storage Access Framework.
- Added native package list, basic package/concept search, hierarchical browsing, expand/collapse, star toggles, package deletion, and outline editing.
- Added native JSON backup import/export entry points.
- Kept the original bundled HTML asset in the project for audit/reference; it is no longer the launcher UI.
- Added feature inventory and migration guide.
- Matched the original JSON backup envelope (`format`, `version`, `exportedAt`, `packages`) and added format/version/package validation before applying imported packages.
- Preserved existing starred state when replacing a package from a text outline or saving an edited outline; fixed nested concept star/delete persistence callbacks.
- Not yet complete: bulk operations, dedicated concept details/parent breadcrumbs, full original search filters/ranking, Room migration, and automated tests.
- Build and device verification were not performed in the available environment.
