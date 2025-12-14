# Release Notes - Gameball Android SDK

This file contains detailed release notes for the latest version. For complete version history, see [CHANGELOG.md](CHANGELOG.md).

---

## Latest Release: v3.1.1

**Release Date**: 2025-12-15
**Version**: 3.1.1
**Type**: Patch Release

---

## 🎉 What's New

v3.1.1 fixes the profile widget to support guest mode, allowing users to explore loyalty features before signing up. All v3.0.0 and v3.1.0 code continues to work without modifications.

### Guest Mode Support

The profile widget now works without requiring customer authentication:

```kotlin
// Show widget without customer ID
val guestRequest = ShowProfileRequest.builder()
    .showCloseButton(true)
    .closeButtonColor("#4CAF50")
    .build()

GameballApp.getInstance(this).showProfile(this, guestRequest)

// Authenticated mode
val customerRequest = ShowProfileRequest.builder()
    .customerId("customer_123")
    .showCloseButton(true)
    .build()

GameballApp.getInstance(this).showProfile(this, customerRequest)
```

### Simplified API

`ShowProfileRequest` builder no longer requires customer ID:

```kotlin
// v3.1.0 - customer ID required
val request = ShowProfileRequest.builder()
    .customerId("customer_123")  // Required
    .build()

// v3.1.1 - customer ID optional
val request = ShowProfileRequest.builder()
    .customerId("customer_123")  // Optional
    .build()

// Guest mode
val guestRequest = ShowProfileRequest.builder().build()
```

---

## 🔄 Changes

- `ShowProfileRequest` builder no longer requires customer ID
- `customerId` parameter is optional (defaults to `null` for guest mode)

---

## Usage Examples

**Conditional Display** - Show guest mode for unauthenticated users:
```kotlin
fun showLoyaltyWidget(activity: Activity) {
    val customerId = getCustomerId() // Your method to get customer ID

    val profileRequest = if (customerId != null) {
        ShowProfileRequest.builder()
            .customerId(customerId)
            .build()
    } else {
        ShowProfileRequest.builder().build()  // Guest mode
    }

    GameballApp.getInstance(activity).showProfile(activity, profileRequest)
}
```

---

## Requirements

- Android API 21+
- Kotlin 2.0.0+
- AndroidX

---

## Migration

No changes required. Existing v3.1.0 and v3.0.0 code works without modifications.

See [MIGRATION.md](MIGRATION.md) for details.

---

## Installation

```kotlin
dependencies {
    implementation 'com.github.gameballers:gb-mobile-android:3.1.1'
}
```

---

## Support

- 📧 Email: support@gameball.co
- 📖 Documentation: https://developer.gameball.co/
- 🐛 Issues: https://github.com/gameballers/gameball-android/issues

---

## Previous Release: v3.1.0

**Release Date**: October 14, 2025
**Version**: 3.1.0
**Type**: Feature Release

---

### What's New

Session Token authentication with per-request override support for enhanced API security and flexibility.

**Key Changes:**
- Singleton pattern with improved initialization
- Session token authentication support
- Per-request token override functionality
- Automatic v4.1 endpoint routing