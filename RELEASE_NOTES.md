# Release Notes - Gameball Android SDK

This file contains detailed release notes for the latest version. For complete version history, see [CHANGELOG.md](CHANGELOG.md).

---

## Latest Release: v3.3.1

**Release Date**: 2026-09-30
**Version**: 3.3.1
**Type**: Patch Release

---

## 🐛 What's Fixed

v3.3.1 is a bug-fix release: the widget's close button no longer lands on the wrong side, and `setLanguage(lang)` takes effect when a preferred language was already set.

### Close Button Direction

The widget's close button is now positioned from the widget's own language alone and pinned to the screen's physical edge: left for Arabic, right for every other language.

Previously its side was chosen by comparing the **device locale** against the widget language, and the button was laid out along the activity's layout direction (start/end). That only worked when the app's layout direction matched the device language and both languages were on the SDK's built-in lists; otherwise the button could land on the wrong side. For example, in an app that doesn't declare `android:supportsRtl`, an Arabic widget on an Arabic device showed it on the right.

### Runtime Language Switching

`setLanguage(lang)` was only setting the SDK's global preferred language, which is resolved *after* the customer's preferred language. When a preferred language had been persisted by an earlier `initializeCustomer`, that value won and the call silently had no effect.

```kotlin
GameballApp.getInstance(this).setLanguage("ar")
```

It now takes precedence, so the change applies to `showProfile` presentations that don't pass their own `lang` and to any other SDK call that resolves language.

### Preferred Language Sync

`setLanguage(lang)` now also mirrors the new language onto the customer's Gameball profile, so server-driven communications (campaigns, emails) follow it as well — previously the change only affected this device. The profile update goes to the most recently initialized customer (remembered across app launches) and is skipped until one has been initialized; to set the language before that, pass it as `preferredLanguage` to `initializeCustomer`.

### Registered Customers Only

The SDK now always initializes customers as registered: `initializeCustomer` sends `guest` as `false`, and `InitializeCustomerRequest.builder().isGuest(...)` is ignored.

---

## 🔄 Changes

- Fixed the widget close button being positioned by device locale and layout direction instead of widget language
- Fixed `setLanguage(lang)` being outranked by a preferred language set through `initializeCustomer`
- `setLanguage(lang)` now mirrors the preferred language onto the customer's Gameball profile
- `initializeCustomer` now always sends `guest: false`; `InitializeCustomerRequest.builder().isGuest(...)` is ignored
- Internal diagnostic logging now only records widget usage
- Removed the undocumented `LanguageUtils.isLtr` and `LanguageUtils.shouldHandleCloseButtonDirection` helpers

---

## Requirements

- Android API 21+
- Kotlin 2.0.0+
- AndroidX

---

## Migration

No code changes required — no public API signature changed. One behavior change: `isGuest(...)` is now ignored, so apps that passed `isGuest(true)` now initialize registered customers.

See [MIGRATION.md](MIGRATION.md) for details.

---

## Installation

```kotlin
dependencies {
    implementation 'com.github.gameballers:gb-mobile-android:3.3.1'
}
```

---

## Support

- 📧 Email: support@gameball.co
- 📖 Documentation: https://developer.gameball.co/
- 🐛 Issues: https://github.com/gameballers/gameball-android/issues

---

## Previous Release: v3.3.0

**Release Date**: 2026-08-29
**Type**: Minor Release

Per-call widget language (`ShowProfileRequest.builder().lang(...)`), a global language switch (`GameballApp.setLanguage(lang)`), and push notification click tracking (`GameballApp.handlePushClick(...)`). See [CHANGELOG.md](CHANGELOG.md) for the full history.
