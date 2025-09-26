# Gameball Android SDK

[![Version](https://img.shields.io/badge/version-3.0.0-blue.svg)](https://github.com/gameballers/gameball-android)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)

The Gameball Android SDK provides a comprehensive solution for integrating customer engagement, loyalty programs, and analytics into your Android applications.

## Features

- 🎯 **Customer Management** - Initialize and manage customer profiles
- 📊 **Event Tracking** - Track user actions and behaviors
- 🎁 **Profile Widget** - Display customer loyalty information
- 🔧 **Modern Architecture** - Built with Kotlin and modern Android patterns
- 🛡️ **Type Safety** - Builder pattern with compile-time validation
- ⚡ **Coroutines Support** - Async operations with structured concurrency

## Requirements

- **Minimum SDK Version**: 21 (Android 5.0)
- **Target SDK Version**: 34
- **Kotlin**: 2.0.0+
- **AndroidX**: Required

## Installation

### Gradle

First, add the required repositories to your project-level `build.gradle` or `settings.gradle` file:

```kotlin
repositories {
    google()
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
    maven { url = URI("https://developer.huawei.com/repo/") }
}
```

Then add the dependency to your app-level `build.gradle` file:

```kotlin
dependencies {
    implementation 'com.gameball:gameball-sdk:3.0.0'
}
```

### Maven

First, add the required repositories to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>google</id>
        <url>https://maven.google.com</url>
    </repository>
    <repository>
        <id>central</id>
        <url>https://repo1.maven.org/maven2</url>
    </repository>
    <repository>
        <id>jitpack</id>
        <url>https://jitpack.io</url>
    </repository>
    <repository>
        <id>huawei</id>
        <url>https://developer.huawei.com/repo/</url>
    </repository>
</repositories>
```

Then add the dependency:

```xml
<dependency>
    <groupId>com.gameball</groupId>
    <artifactId>gameball-sdk</artifactId>
    <version>3.0.0</version>
</dependency>
```

## Quick Start

### 1. Initialize the SDK

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = GameballConfig.builder()
            .apiKey("your-api-key")
            .lang("en")
            .platform("android")
            .shop("your-shop-id")
            .build()

        GameballApp.getInstance(this).init(config)
    }
}
```

### 2. Initialize Customer

```kotlin
val customerRequest = InitializeCustomerRequest.builder()
    .customerId("customer-123")
    .email("customer@example.com")
    .mobileNumber("+1234567890")
    .deviceToken("fcm-device-token")
    .pushProvider(PushProvider.Firebase)
    .build()

GameballApp.getInstance(this).initializeCustomer(
    customerRequest,
    object : Callback<InitializeCustomerResponse> {
        override fun onSuccess(response: InitializeCustomerResponse) {
            // Handle success
        }

        override fun onError(error: Throwable) {
            // Handle error
        }
    }
)
```

### 3. Track Events

```kotlin
val event = Event.builder()
    .customerId("customer-123")
    .eventName("purchase")
    .eventMetaData("product_id", "12345")
    .eventMetaData("amount", 99.99)
    .build()

GameballApp.getInstance(this).sendEvent(
    event,
    object : Callback<Boolean> {
        override fun onSuccess(success: Boolean) {
            // Event tracked successfully
        }

        override fun onError(error: Throwable) {
            // Handle error
        }
    }
)
```

### 4. Show Profile Widget

```kotlin
val profileRequest = ShowProfileRequest.builder()
    .customerId("customer-123")
    .showCloseButton(true)
    .hideNavigation(false)
    .closeButtonColor("#FF0000")
    .openDetail("details_earn")
    .build()

GameballApp.getInstance(this).showProfile(this, profileRequest)
```

## API Methods

The SDK provides the following public methods:

- `init(config: GameballConfig)` - Initialize the SDK
- `initializeCustomer(request, callback)` - Register/initialize customer
- `sendEvent(event, callback)` - Track events
- `showProfile(activity, request)` - Show profile widget

## Advanced Usage

### Customer Attributes

```kotlin
val attributes = CustomerAttributes.builder()
    .displayName("John Doe")
    .firstName("John")
    .lastName("Doe")
    .email("john@example.com")
    .gender("male")
    .dateOfBirth("1990-01-01")
    .preferredLanguage("en")
    .addCustomAttribute("tier", "premium")
    .build()

val customerRequest = InitializeCustomerRequest.builder()
    .customerId("customer-123")
    .customerAttributes(attributes)
    .build()
```

### Push Notifications

```kotlin
// For Firebase Cloud Messaging
val customerRequest = InitializeCustomerRequest.builder()
    .customerId("customer-123")
    .deviceToken("fcm-token")
    .pushProvider(PushProvider.Firebase)
    .build()

// For Huawei Push Kit
val customerRequest = InitializeCustomerRequest.builder()
    .customerId("customer-123")
    .deviceToken("hms-token")
    .pushProvider(PushProvider.Huawei)
    .build()
```

### Error Handling

```kotlin
GameballApp.getInstance(this).initializeCustomer(
    customerRequest,
    object : Callback<InitializeCustomerResponse> {
        override fun onSuccess(response: InitializeCustomerResponse) {
            Log.d("Gameball", "Customer initialized: ${response.customerId}")
        }

        override fun onError(error: Throwable) {
            when (error) {
                is IllegalArgumentException -> {
                    Log.e("Gameball", "Invalid input: ${error.message}")
                }
                is IllegalStateException -> {
                    Log.e("Gameball", "SDK not properly initialized: ${error.message}")
                }
                else -> {
                    Log.e("Gameball", "Network error: ${error.message}")
                }
            }
        }
    }
)
```

## Migration from v2.x

⚠️ **Version 3.0.0 contains breaking changes**. Please see the [Migration Guide](MIGRATION.md) for detailed upgrade instructions.

### Key Changes

- **New Builder Pattern**: All request models now use builder pattern
- **Kotlin-First**: SDK migrated to Kotlin with improved type safety
- **Unified API**: Simplified method signatures and consistent naming
- **Enhanced Validation**: Better error handling and input validation
- **Removed Deprecated Features**:
  - **Firebase Dynamic Links functionality** (deprecated in v2.2.0)
  - **Legacy Java-based request models** (`CustomerRegisterRequest`)
  - **Multiple method overloads** in favor of builder pattern
  - **Legacy `registerCustomer` method variants**

## Configuration Options

### GameballConfig

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | ✅ **Required** | Your Gameball API key |
| `lang` | String | ✅ **Required** | Language code (e.g., "en", "ar") |
| `platform` | String | ❌ Optional | Platform identifier |
| `shop` | String | ❌ Optional | Shop identifier |
| `apiPrefix` | String | ❌ Optional | Custom API endpoint prefix |

**GameballConfig Validation Rules:**
- API key is required (cannot be null)
- Language is required (cannot be null)

### InitializeCustomerRequest

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `customerId` | String | ✅ **Required** | Unique customer identifier |
| `email` | String | ❌ Optional | Customer email address |
| `mobile` | String | ❌ Optional | Customer mobile number |
| `deviceToken` | String | ❌ Optional | Push notification token |
| `pushProvider` | PushProvider | ❌ Optional | Push service provider (Firebase/Huawei) |
| `customerAttributes` | CustomerAttributes | ❌ Optional | Additional customer data |
| `referralCode` | String | ❌ Optional | Referral code |
| `isGuest` | Boolean | ❌ Optional | Guest user flag (defaults to false) |
| `osType` | String | ❌ Auto-set | Operating system type (automatically set to "Android") |

**InitializeCustomerRequest Validation Rules:**
- Customer ID cannot be empty
- If push provider is set, device token is required
- If device token is set, push provider is required

### ShowProfileRequest

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `customerId` | String | ✅ **Required** | Unique customer identifier |
| `openDetail` | String | ❌ Optional | Detail section to open (e.g., "details_earn") |
| `hideNavigation` | Boolean | ❌ Optional | Hide navigation bar |
| `showCloseButton` | Boolean | ❌ Optional | Show close button |
| `closeButtonColor` | String | ❌ Optional | Close button color (hex format) |
| `widgetUrlPrefix` | String | ❌ Optional | Custom widget URL prefix |
| `capturedLinkCallback` | Callback<String> | ❌ Optional | Callback for captured links |

**ShowProfileRequest Validation Rules:**
- Customer ID cannot be empty

### CustomerAttributes

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `displayName` | String | ❌ Optional | Customer's display name |
| `firstName` | String | ❌ Optional | Customer's first name |
| `lastName` | String | ❌ Optional | Customer's last name |
| `email` | String | ❌ Optional | Customer's email address |
| `gender` | String | ❌ Optional | Customer's gender |
| `mobile` | String | ❌ Optional | Customer's mobile number |
| `dateOfBirth` | String | ❌ Optional | Date of birth (YYYY-MM-DD format) |
| `joinDate` | String | ❌ Optional | Customer join date |
| `preferredLanguage` | String | ❌ Optional | Preferred language code |
| `channel` | String | ❌ Optional | Channel identifier (defaults to "mobile") |
| `customAttributes` | Map<String, String> | ❌ Optional | Custom key-value attributes |
| `additionalAttributes` | Map<String, String> | ❌ Optional | Additional key-value attributes |

**CustomerAttributes Builder Methods:**
- `addCustomAttribute(key, value)` - Adds a custom attribute
- `addAdditionalAttribute(key, value)` - Adds an additional attribute
- `mobileNumber(number)` - Sets mobile number (also has `getMobileNumber()` getter)
- All other properties have standard setter methods

### Event

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `customerId` | String | ✅ **Required** | Unique customer identifier |
| `eventName` | String | ✅ **Required** | Name of the event to track |
| `eventMetaData` | Key-Value pairs | ❌ Optional | Event metadata (added via multiple calls) |
| `email` | String | ❌ Optional | Customer email address |
| `mobile` | String | ❌ Optional | Customer mobile number |

**Event Builder Methods:**
- `eventName(name)` - Sets the current event name (required before adding metadata)
- `eventMetaData(key, value)` - Adds metadata to the current event
- `customerId(id)` - Sets customer ID
- `email(email)` - Sets customer email
- `mobileNumber(mobile)` - Sets customer mobile number

**Event Validation Rules:**
- Customer ID cannot be empty
- Event name must be set before adding metadata
- At least one event must be added

## Proguard/R8

If you're using code obfuscation, add these rules to your `proguard-rules.pro`:

```proguard
# Gameball SDK
-keep class com.gameball.gameball.** { *; }
-keep class com.gameball.gameball.model.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
```

## Troubleshooting

### Common Issues

1. **SDK Not Initialized**
   ```
   Error: API key is required for customer initialization
   ```
   **Solution**: Ensure you call `GameballApp.getInstance(context).init(config)` before other SDK methods.

2. **Invalid Customer ID**
   ```
   Error: Customer ID cannot be blank
   ```
   **Solution**: Provide a non-empty customer ID when building requests.

3. **Network Errors**
   - Check your internet connection
   - Verify your API key is correct
   - Ensure your server can reach Gameball endpoints

### Debug Logging

The SDK automatically enables debug logging in debug builds. Network requests and responses are logged when `BuildConfig.DEBUG` is true in your app. No additional configuration is needed.

To view SDK logs, filter by these tags:
- `GameballApp` - General SDK operations
- `LoggingInterceptor` - Network requests/responses
- `HeaderInterceptor` - Request headers

## Documentation

- 📋 **[Changelog](CHANGELOG.md)** - Version history and release notes
- 🔄 **[Migration Guide](MIGRATION.md)** - Upgrade from v2.x to v3.0.0
- 📝 **[Release Notes](RELEASE_NOTES.md)** - Latest release details

## Support

- 📧 **Email**: support@gameball.co
- 📖 **Documentation**: [https://docs.gameball.co](https://docs.gameball.co)
- 🐛 **Issues**: [GitHub Issues](https://github.com/gameballers/gameball-android/issues)

## License

```
MIT License

Copyright (c) 2024 Gameball

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```