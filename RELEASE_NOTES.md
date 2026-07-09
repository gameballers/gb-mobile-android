# Release Notes - Gameball Android SDK

This file contains detailed release notes for the latest version. For complete version history, see [CHANGELOG.md](CHANGELOG.md).

---

## Latest Release: v3.2.1

**Release Date**: 2026-07-09
**Version**: 3.2.1
**Type**: Patch Release

---

## 🐛 What's Fixed

v3.2.1 is a bug-fix release. On devices where the system draws a status bar or display cutout over the app window — most visibly on Pixel devices and Android 15 — the widget's header row was rendered underneath it. The close buttons were partially covered and could not be tapped.

### Widget Header Under the Status Bar

The widget activity uses a translucent theme and therefore always draws edge-to-edge, behind the status bar. The widget content is now padded at the top by the status-bar / display-cutout height, so the header and its close buttons render below the system bar and remain tappable.

Because translucent windows do not reliably dispatch `WindowInsets`, the padding is applied immediately from the framework's `status_bar_height` resource and then refined from the real inset (including tall cutouts) once the system dispatches it.

### Status-Bar Icon Contrast

`GameballWidgetActivity` now uses a dedicated `Theme.GameballWidget` theme that enables `windowLightStatusBar`, so the system's status-bar icons stay dark and visible over the widget's white top band. The theme is kept separate from `Theme.Transparent` so `LargeNotificationActivity` is unaffected.

---

## 🔄 Changes

- Padded widget content by the status-bar / display-cutout inset so top buttons stay tappable
- Added `Theme.GameballWidget` (dark status-bar icons) and applied it to `GameballWidgetActivity`

---

## Requirements

- Android API 21+
- Kotlin 2.0.0+
- AndroidX

---

## Migration

No changes required. v3.2.1 is a drop-in replacement for v3.2.0 — no public API changed.

See [MIGRATION.md](MIGRATION.md) for details.

---

## Installation

```kotlin
dependencies {
    implementation 'com.github.gameballers:gb-mobile-android:3.2.1'
}
```

---

## Support

- 📧 Email: support@gameball.co
- 📖 Documentation: https://developer.gameball.co/
- 🐛 Issues: https://github.com/gameballers/gameball-android/issues

---

## Previous Release: v3.2.0

**Release Date**: 2026-06-17
**Type**: Minor Release

Widget event channel (`ShowProfileRequest.widgetEventCallback`), widget dismissal controls (`GameballApp.hideProfile()` and `window.GameballWidget.closeWidget()`), external-link handling, optional `mobile` / `email` channel-merging parameters, and internal diagnostic logging. See [CHANGELOG.md](CHANGELOG.md) for the full history.
