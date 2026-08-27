# Release Notes - Gameball Android SDK

This file contains detailed release notes for the latest version. For complete version history, see [CHANGELOG.md](CHANGELOG.md).

---

## Latest Release: v3.3.0

**Release Date**: 2026-08-29
**Version**: 3.3.0
**Type**: Minor Release

---

## ✨ What's New

v3.3.0 adds **per-call and global language control** and **push notification click tracking**. All v3.2.x and v3.1.x code continues to work without modification — every addition is backward compatible.

### Per-Call Widget Language

`ShowProfileRequest.builder()` now accepts an optional `lang` (2-letter code, e.g. `"en"`, `"ar"`) to present that one widget in a specific language:

```kotlin
val request = ShowProfileRequest.builder()
    .customerId("customer_123")
    .lang("ar")
    .build()

GameballApp.getInstance(this).showProfile(this, request)
```

When `lang` is omitted, the SDK's existing resolution applies: customer preferred language, then global preferred language, then device locale.

### Global Language Switch

`GameballApp.setLanguage(lang)` changes the SDK's global language on demand, without re-calling `init`:

```kotlin
GameballApp.getInstance(this).setLanguage("ar")
```

This changes the fallback used by future `showProfile` presentations that don't pass their own `lang` (a per-call `lang` still wins) and any other SDK call that resolves language. Invalid codes are ignored.

### Push Click Tracking

`GameballApp.handlePushClick(payload, callback?, sessionToken?)` reports taps on Gameball push notifications so campaign clicks are counted. Call it from your notification-tap handler with the notification's FCM data payload (e.g. `RemoteMessage.data`, or the launcher intent extras when the system tray showed the notification):

```kotlin
val isGameball = GameballApp.getInstance(this).handlePushClick(
    payload = remoteMessage.data,
    callback = object : Callback<Boolean> {
        override fun onSuccess(result: Boolean) { Log.d(TAG, "Click reported") }
        override fun onError(t: Throwable) { Log.e(TAG, "Click report failed", t) }
    }
)

if (!isGameball) {
    // Not a Gameball notification — run your own handling.
}
```

It returns `true` when the notification is a Gameball one; the tap is reported to Gameball when the payload carries a click token. An optional `sessionToken` overrides the global session token for this request.

---

## 🔄 Changes

- Added optional `ShowProfileRequest.builder().lang(...)` (per-presentation language override)
- Added `GameballApp.setLanguage(lang)` (global language switch)
- Added `GameballApp.handlePushClick(payload, callback?, sessionToken?)` (push click tracking)

---

## Requirements

- Android API 21+
- Kotlin 2.0.0+
- AndroidX

---

## Migration

No changes required. v3.3.0 is a drop-in upgrade from v3.2.x — no existing public API changed.

See [MIGRATION.md](MIGRATION.md) for details.

---

## Installation

```kotlin
dependencies {
    implementation 'com.github.gameballers:gb-mobile-android:3.3.0'
}
```

---

## Support

- 📧 Email: support@gameball.co
- 📖 Documentation: https://developer.gameball.co/
- 🐛 Issues: https://github.com/gameballers/gameball-android/issues

---

## Previous Release: v3.2.1

**Release Date**: 2026-07-09
**Type**: Patch Release

Widget content is now padded by the status-bar / display-cutout height so the header close buttons stay tappable, and `GameballWidgetActivity` uses a dedicated `Theme.GameballWidget` with dark status-bar icons. See [CHANGELOG.md](CHANGELOG.md) for the full history.
