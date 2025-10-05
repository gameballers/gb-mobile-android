# Release Notes - Gameball Android SDK

This file contains detailed release notes for the latest version. For complete version history, see [CHANGELOG.md](CHANGELOG.md).

---

## Latest Release: v3.1.0

**Release Date**: October 14, 2025
**Version**: 3.1.0
**Type**: Feature Release

---

## 🎉 What's New

Gameball Android SDK v3.1.0 introduces **Session Token authentication with per-request override support** for enhanced API security and flexibility. This feature release adds optional token-based authentication with automatic secure endpoint routing, plus the ability to override authentication on a per-request basis.

### 🔒 Security Enhancements

- **Session Token Authentication**: Optional token-based authentication mechanism for secure API communication
- **Per-Request Token Override**: Control authentication on individual API calls for maximum flexibility
- **Automatic Secure Routing**: SDK automatically switches from API v4.0 to v4.1 endpoints when session token is provided
- **Secure Header Transmission**: `X-GB-TOKEN` header added to requests when using session token authentication
- **Backward Compatible**: Existing implementations continue to work without any changes

### 🛠️ Developer Experience

- **Simple Configuration**: Add `sessionToken` to your `GameballConfig` to enable secure authentication globally
- **Flexible Control**: Override session token on individual method calls for granular authentication control
- **Transparent Security**: No code changes required beyond initial configuration
- **Easy Token Management**: Pass `null` to clear session token for specific requests

---

## 🚀 Key Features

### Session Token Authentication

Enable secure authentication by adding the `sessionToken` parameter to your SDK configuration:

```kotlin
val config = GameballConfig.builder()
    .apiKey("your-api-key")
    .lang("en")
    .sessionToken("your-secure-session-token")  // Optional: Enable secure authentication
    .build()

GameballApp.getInstance(this).init(config)
```

When a session token is provided:
- All API requests automatically route to secure v4.1 endpoints
- `X-GB-TOKEN` header is included in all authenticated requests
- Enhanced security for customer data and API communications

### Per-Request Session Token Override

Override the global session token for individual API calls:

```kotlin
// Initialize customer with a different session token
val customerRequest = InitializeCustomerRequest.builder()
    .customerId("customer123")
    .build()

gameballApp.initializeCustomer(
    customerRequest,
    callback,
    sessionToken = "different-token"  // Override global token
)

// Send an event without authentication (clear token for this request)
val event = Event.builder()
    .eventName("purchase")
    .build()

gameballApp.sendEvent(
    event,
    callback,
    sessionToken = null  // Clear token for this request
)

// Show profile with custom token
val profileRequest = ShowProfileRequest.builder()
    .customerId("customer123")
    .build()

gameballApp.showProfile(
    activity,
    profileRequest,
    sessionToken = "user-specific-token"
)
```

**Important Note:** The `sessionToken` parameter must be explicitly passed to **every method call** where you want to use a specific token. Passing it once does not persist across subsequent method calls. Each method call independently uses the token you provide in that call, or falls back to the global session token if no parameter is provided.

### Standard Configuration (Without Token)

```kotlin
val config = GameballConfig.builder()
    .apiKey("your-api-key")
    .lang("en")
    .platform("android")
    .shop("your-shop-id")
    .build()

GameballApp.getInstance(this).init(config)
```

---

## ⚠️ Breaking Changes

**None.** This is a backward-compatible feature release. All existing v3.0.0 implementations continue to work without modification.

---

## 📈 What's Changed

### Security Improvements
- **Enhanced API Security**: Session Token authentication adds an additional security layer for sensitive operations
- **Automatic Endpoint Management**: Smart routing to secure endpoints when authentication is enabled
- **Secure Token Storage**: Session tokens are securely stored and managed via SharedPreferences

---

## 🔧 Technical Details

### Requirements
- **Minimum SDK**: Android API 21 (Android 5.0)
- **Target SDK**: Android API 34
- **Kotlin**: 2.0.0+
- **AndroidX**: Required

### New Configuration Option

#### GameballConfig

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `sessionToken` | String | ❌ Optional | Session Token for secure authentication |

### Internal Changes
- Added `GB_TOKEN_PREFERENCE` to SharedPreferencesUtils for secure token storage
- HeaderInterceptor now conditionally adds `X-GB-TOKEN` header when session token is present
- Automatic API version switching logic (v4.0 → v4.1) in HeaderInterceptor
- API version constants added to Config class (`API_V4_0`, `API_V4_1`)

---

## 🛡️ Security & Reliability

### Authentication Security
- **Optional Session Token**: Adds token-based authentication layer when needed
- **Automatic Secure Routing**: Transparent upgrade to secure v4.1 endpoints
- **Header Security**: Secure transmission of authentication tokens via HTTP headers
- **Token Management**: Secure storage and lifecycle management of session tokens

---

## 📚 Upgrading from v3.0.0

### No Migration Required

This is a backward-compatible release. Your existing v3.0.0 code will continue to work without any changes.

### To Enable Session Token Authentication (Optional)

Simply add the `sessionToken` parameter to your existing configuration:

```kotlin
// Before (v3.0.0) - Still works in v3.1.0
val config = GameballConfig.builder()
    .apiKey("your-api-key")
    .lang("en")
    .build()

// After (v3.1.0) - With optional Session Token
val config = GameballConfig.builder()
    .apiKey("your-api-key")
    .lang("en")
    .sessionToken("your-secure-session-token")  // Add this line
    .build()
```

### Support
- 📧 **Email**: support@gameball.co
- 📖 **Documentation**: [https://developer.gameball.co/](https://developer.gameball.co/)
- 🐛 **Issues**: [GitHub Issues](https://github.com/gameballers/gameball-android/issues)

---

## 🎯 What's Next

### Future Enhancements
- Enhanced analytics capabilities
- Additional security features
- Performance optimizations
- New integration features

### Roadmap
- Version 3.2.0: Enhanced analytics and reporting
- Future versions: Continued improvements and new features

---

## 📦 Installation

### Gradle
```kotlin
dependencies {
    implementation 'com.gameball:gameball-sdk:3.1.0'
}
```

### Maven
```xml
<dependency>
    <groupId>com.gameball</groupId>
    <artifactId>gameball-sdk</artifactId>
    <version>3.1.0</version>
</dependency>
```

---

## 🏆 Benefits Summary

✅ **Enhanced Security**: Optional Session Token authentication for sensitive operations
✅ **Backward Compatible**: Zero migration effort - existing code continues to work
✅ **Automatic Routing**: Smart endpoint selection based on authentication status
✅ **Simple Configuration**: One-line addition to enable secure authentication
✅ **Flexible**: Use token authentication only when needed
✅ **Transparent**: No code changes beyond initial configuration

---

## ⭐ Acknowledgments

We thank our development community for their feedback on security features.

---

**Ready to upgrade?** Simply update your dependency to v3.1.0. No migration required!

*For technical support, contact us at support@gameball.co*