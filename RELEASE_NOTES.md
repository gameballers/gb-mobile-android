# Release Notes - Gameball Android SDK

This file contains detailed release notes for the latest version. For complete version history, see [CHANGELOG.md](CHANGELOG.md).

---

## Latest Release: v4.0.0

**Release Date**: 2026-10-22
**Version**: 4.0.0
**Type**: Major Release

---

## 🎉 What's New

v4.0.0 raises the minimum supported Android version to **API 24 (Android 7.0)**, and adds **in-app messaging**: campaigns authored in the Gameball dashboard, displayed inside your app. In-app messaging is opt-in — until you call `startInAppMessaging` it makes no requests, starts no timers, stores nothing, draws nothing and registers no lifecycle callbacks.

- **In-app messaging**: modal, slideup and fullscreen campaigns from the Gameball dashboard, shown inside your app
- **Triggers**: session start, and custom events filtered on their metadata
- **Purchase logging**: `logPurchase(...)` for purchase-triggered campaigns
- **Hooks**: `beforeDisplay`, `onAction`, `onNavigate` and `observer` to control display, taps and navigation
- **Raised minimum**: Android API 24+ (Android 7.0)

---

## 🚀 Key Features

### 💬 Opting in

```kotlin
val gameball = GameballApp.getInstance(context)
gameball.init(config)
gameball.initializeCustomer(customerRequest, callback)

gameball.startInAppMessaging("customer-123")

// On logout
gameball.stopInAppMessaging()
```

```java
GameballApp gameball = GameballApp.getInstance(context);
gameball.startInAppMessaging("customer-123");
```

Starting for a different customer refetches campaigns and resets frequency caps, so you don't need to stop first.

### 🖼️ Message types

**Modal** — a card over a dimmed background, with up to two buttons.

**Slideup** — a banner at the top or bottom edge. It doesn't block the app, carries no buttons (the banner itself is the tap target), and is dismissed by swiping toward its own edge.

**Fullscreen** — edge to edge. A fullscreen campaign can insist on portrait or landscape, and waits until the device is turned that way.

### 🎯 Triggers

**Session start** fires on launch, and again when the app returns to the foreground after more than the session timeout (30 seconds by default; set it with `InAppMessagingOptions.builder().sessionTimeoutSeconds(...)`).

**Custom events** match on the event name and, optionally, on the event's metadata. Events you already send through `sendEvent` reach the trigger engine automatically:

```kotlin
gameball.sendEvent(
    Event.builder()
        .customerId("customer-123")
        .eventName("place_order")
        .eventMetaData("total", 250)
        .build(),
    callback
)
```

### 🛒 Purchase logging

```kotlin
gameball.logPurchase(
    customerId = "customer-123",
    productId = "sku-42",
    price = 250.0,
    currency = "USD"
)
```

`logPurchase` sends a `purchase` event to Gameball with `productId`, `price`, `currency` and `quantity` as metadata, whether or not in-app messaging has started, so purchase-triggered campaigns can filter on them.

### 🚦 What controls whether a message shows

A backend-controlled minimum interval between any two messages, per-campaign repeat rules that persist across restarts, campaign expiry and priority, and quiet hours. A message that can't be shown yet — because the Gameball profile widget or another message is open — waits and shows as soon as it can.

### 🖐️ Hooks

```kotlin
val options = InAppMessagingOptions.builder()
    // Postpone during checkout rather than interrupting it.
    .beforeDisplay { message ->
        if (checkoutInProgress) DisplayDecision.LATER else DisplayDecision.SHOW
    }
    // Return true if you handled the tap yourself.
    .onAction { message, button, action -> false }
    // Route a campaign's navigate action through your own navigation.
    .onNavigate { route, arguments -> navController.navigate(route) }
    // See every message the SDK selects.
    .observer { message -> analytics.log("gb_message_selected", message.campaignId) }
    .build()

gameball.startInAppMessaging("customer-123", options)
```

Every hook is optional and individually guarded: if one throws, the SDK falls back to its own handling and the message still shows. A tap is reported to Gameball whatever `onAction` returns.

### 📊 Analytics and personalisation

Impressions, clicks and dismissals are reported automatically, batched and kept on the device until sent. Campaign text can contain `{token}` placeholders, filled with the customer's current values just before display (waiting at most two seconds). If the values don't arrive, the placeholders are removed rather than shown as raw `{braces}`, and the message still shows.

### 🔍 Diagnostics

```kotlin
GameballApp.inAppMessagingLogging = true      // Kotlin
```

```java
GameballApp.setInAppMessagingLogging(true);   // Java
```

In-app messaging logs to logcat under the tag `GameballIAM`: on in debuggable builds, off in release builds. Turn it on in a release build only to reproduce a reported problem.

### ⚠️ Availability

In-app messaging needs the in-app messaging endpoints enabled for your account. Where they aren't, the SDK logs the error in debuggable builds and stays silent — nothing surfaces to your app.

---

## ⚠️ Breaking Changes

**This is a major release.** Apps whose `minSdk` is below 24 must raise it before upgrading.

- **Android**: minimum raised from API 21 (Android 5.0) to **API 24 (Android 7.0)**

There are no changes to the public API — every v3.x call keeps its signature and behaviour.

---

## 🔄 Changes

- Raised the minimum supported Android version to API 24 (Android 7.0)
- Added in-app messaging: `startInAppMessaging(customerId, options)`, `stopInAppMessaging()` and `isInAppMessagingStarted()`
- Added modal, slideup and fullscreen messages, triggered on session start or by custom events with metadata filters
- Added `logPurchase(...)` to send purchase events that campaigns can target
- Added `beforeDisplay`, `onAction`, `onNavigate` and `observer` hooks through `InAppMessagingOptions`
- Added automatic impression, click and dismissal analytics, kept on the device until sent
- Added `GameballApp.inAppMessagingLogging` to control in-app messaging logcat output

---

## 🔧 Technical Details

### Requirements

- Android API 24+ (Android 7.0)
- Kotlin 2.0.0+
- AndroidX

---

## 📚 Migration Support

Raise your app's `minSdk` to 24. No code changes are required — all v3.x calls work without modification. In-app messaging is opt-in; to adopt it, call `startInAppMessaging(customerId)` once the customer is known and `stopInAppMessaging()` on logout.

### Migration Resources
- **[Migration Guide](MIGRATION.md#migration-guide-v3x--v400)**: Step-by-step migration instructions
- **[README](README.md)**: Complete usage documentation with examples
- **[Changelog](CHANGELOG.md)**: Detailed list of all changes

### Support

- 📧 Email: support@gameball.co
- 📖 Documentation: https://developer.gameball.co/
- 🐛 Issues: https://github.com/gameballers/gb-mobile-android/issues

---

## 📦 Installation

```kotlin
dependencies {
    implementation 'com.github.gameballers:gb-mobile-android:4.0.0'
}
```

---

## Previous Release: v3.3.1

**Release Date**: 2026-10-19
**Type**: Patch Release

Fixed the widget close button direction and `setLanguage(lang)` being outranked by an earlier preferred language; `setLanguage(lang)` now mirrors the language onto the customer's Gameball profile, unsupported language codes fall back to `en`, and customers are always initialized as registered. See [CHANGELOG.md](CHANGELOG.md) for the full history.
