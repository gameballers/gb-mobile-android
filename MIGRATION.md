# Migration Guide: Gameball Android SDK

This guide provides migration instructions for upgrading between major versions of the Gameball Android SDK.

---

## Table of Contents

- [v3.0.0 → v3.1.0](#migration-guide-v300--v310)
- [v2.x → v3.0.0](#migration-guide-v2x--v300)

---

## Migration Guide: v3.0.0 → v3.1.0

Version 3.1.0 introduces security enhancements. This is a **minor update** with no breaking changes.

### Overview of Changes

#### 🔒 What's New
- **Token-based authentication** with automatic endpoint versioning for improved security
- **Per-request session token override** for flexible authentication control on individual API calls

### Update Dependencies

Update your dependency to v3.1.0:

```kotlin
dependencies {
    implementation 'com.gameball:gameball-sdk:3.1.0'
}
```

### Security Enhancement: Session Token Authentication

Version 3.1.0 introduces **optional Session Token authentication** for enhanced API security. This feature is completely **backward-compatible**—existing implementations continue to work without any changes.

#### When to Use Session Token Authentication

Session Token authentication provides an additional security layer for:
- Production environments handling sensitive customer data
- Applications requiring enhanced API security
- Compliance with security standards and regulations

#### Enabling Session Token Authentication (Optional)

**Without Session Token (Standard Configuration - v3.0.0 style):**
```kotlin
val config = GameballConfig.builder()
    .apiKey("your-api-key")
    .lang("en")
    .platform("android")
    .shop("your-shop-id")
    .build()

GameballApp.getInstance(this).init(config)
```

**With Session Token (Enhanced Security - v3.1.0):**
```kotlin
val config = GameballConfig.builder()
    .apiKey("your-api-key")
    .lang("en")
    .platform("android")
    .shop("your-shop-id")
    .sessionToken("your-secure-session-token")  // Optional: Add for secure authentication
    .build()

GameballApp.getInstance(this).init(config)
```

#### How Session Token Authentication Works

When a session token is provided, the SDK automatically:

1. **Secure Endpoint Routing**: Switches from API v4.0 to v4.1 endpoints
2. **Header Authentication**: Adds `X-GB-TOKEN` header to all API requests
3. **Secure Storage**: Stores token securely using Android SharedPreferences
4. **Lifecycle Management**: Handles token validation and session management

#### Per-Request Session Token Override (New in v3.1.0)

You can now override or clear the session token for individual API calls:

```kotlin
// Override session token for a specific customer initialization
val customerRequest = InitializeCustomerRequest.builder()
    .customerId("customer-123")
    .build()

gameballApp.initializeCustomer(
    customerRequest,
    callback,
    sessionToken = "user-specific-token"  // Override global token
)

// Clear session token for a specific event (anonymous tracking)
val event = Event.builder()
    .eventName("page_view")
    .build()

gameballApp.sendEvent(
    event,
    callback,
    sessionToken = null  // Clear token for this request
)

// Show profile with different authentication
val profileRequest = ShowProfileRequest.builder()
    .customerId("customer-123")
    .build()

gameballApp.showProfile(
    activity,
    profileRequest,
    sessionToken = "temporary-token"
)
```

**Important Note:** The `sessionToken` parameter must be explicitly passed to **every method call** where you want to use a specific token. Passing it once does not persist across subsequent method calls. Each method call independently uses the token you provide in that call, or falls back to the global session token if no parameter is provided.

#### Security Benefits

✅ **Token-Based Authentication**: Additional authentication layer beyond API key
✅ **Per-Request Control**: Override or clear tokens for individual API calls
✅ **Flexible Authentication**: Support multi-user scenarios and anonymous actions
✅ **Automatic Secure Routing**: No manual endpoint configuration required
✅ **Transparent Security**: Once configured, works automatically without code changes
✅ **Backward Compatible**: Enable only when needed; existing code works unchanged

> **Note**: Session Token is optional. If you don't provide it, the SDK continues to work with standard API v4.0 endpoints using API key authentication only.

### Migration Checklist

- [ ] Update Gradle dependency to v3.1.0
- [ ] (Optional) Add `sessionToken` to your GameballConfig if needed
- [ ] Verify all SDK functionality works correctly
- [ ] Test push notifications
- [ ] Test profile widget displays correctly
- [ ] Test event tracking

### Benefits After Migration

After upgrading to v3.1.0, you'll benefit from:

✅ **Optional Enhanced Security**: GB Token authentication when needed

✅ **Automatic Secure Routing**: SDK automatically uses secure endpoints when token is provided

✅ **Backward Compatible**: Existing code continues to work without changes

---

## Migration Guide: v2.x → v3.0.0

This guide will help you migrate from Gameball Android SDK v2.x to v3.0.0. Version 3.0.0 introduces significant improvements with modern Kotlin architecture, but requires some code changes due to breaking changes.

## Overview of Changes

### 🔧 What's New
- **Complete Kotlin rewrite** with enhanced type safety
- **Builder pattern** for all request models with compile-time validation
- **Improved error handling** with proper callback mechanisms
- **Unified API design** with consistent method signatures
- **Enhanced performance** through optimized internal architecture

### ⚠️ Breaking Changes
- Migration from Java to Kotlin
- New builder pattern for all requests
- Method signature changes
- Updated configuration approach
- Removed deprecated functionality:
  - **Firebase Dynamic Links functionality** (deprecated in v2.2.0)
  - **Legacy Java-based request models** (`CustomerRegisterRequest`)
  - **Multiple method overloads** in favor of builder pattern
  - **Direct parameter-based initialization methods**
  - **Legacy `registerCustomer` method variants**

---

## Step-by-Step Migration

### 1. Update Dependencies

**Before (v2.x):**
```kotlin
dependencies {
    implementation 'com.gameball:gameball-sdk:2.3.0'
}
```

**After (v3.0.0):**

First, add the required repositories to your project-level `build.gradle` or `settings.gradle` file:

```kotlin
repositories {
    google()
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
    maven { url = URI("https://developer.huawei.com/repo/") }
}
```

Then update the dependency:

```kotlin
dependencies {
    implementation 'com.gameball:gameball-sdk:3.0.0'
}
```

### 2. SDK Initialization

**Before (v2.x):**
```java
// Java/Kotlin
GameballApp.getInstance(this).init(CLIENT_ID, EXTERNAL_ID, NOTIFICATION_ICON);
```

**After (v3.0.0):**
```kotlin
// Kotlin only
val config = GameballConfig.builder()
    .apiKey("your-api-key")
    .lang("en")
    .platform("android")
    .shop("your-shop-id")
    .build()

GameballApp.getInstance(this).init(config)
```

### 3. Customer Registration/Initialization

**Before (v2.x):**
```java
// Direct method calls with parameters
GameballApp.getInstance(this).registerCustomer(
    customerId,
    email,
    mobile,
    attributes,
    callback
);
```

**After (v3.0.0):**
```kotlin
// Builder pattern with comprehensive options
val customerRequest = InitializeCustomerRequest.builder()
    .customerId("customer-123")
    .email("customer@example.com")
    .mobileNumber("+1234567890")
    .deviceToken("fcm-device-token")
    .pushProvider(PushProvider.FCM)
    .customerAttributes(attributes)
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

### 4. Customer Attributes

**Before (v2.x):**
```java
// Direct constructor or setter methods
CustomerAttributes attributes = new CustomerAttributes();
attributes.setDisplayName("John Doe");
attributes.setEmail("john@example.com");
// ... other properties
```

**After (v3.0.0):**
```kotlin
// Builder pattern with type safety
val attributes = CustomerAttributes.builder()
    .displayName("John Doe")
    .firstName("John")
    .lastName("Doe")
    .email("john@example.com")
    .gender("male")
    .dateOfBirth("1990-01-01")
    .preferredLanguage("en")
    .addCustomAttribute("tier", "premium")
    .addAdditionalAttribute("source", "mobile")
    .build()
```

### 5. Event Tracking

**Before (v2.x):**
```java
// Method with individual parameters
GameballApp.getInstance(this).sendEvent(
    eventName,
    metadata,
    customerId,
    callback
);
```

**After (v3.0.0):**
```kotlin
// Builder pattern with structured metadata
val event = Event.builder()
    .customerId("customer-123")
    .eventName("purchase")
    .eventMetaData("product_id", "12345")
    .eventMetaData("amount", 99.99)
    .eventMetaData("category", "electronics")
    .email("customer@example.com")
    .mobileNumber("+1234567890")
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

### 6. Profile Widget

**Before (v2.x):**
```java
// Direct method with parameters
GameballApp.getInstance(this).showProfile(
    activity,
    customerId,
    showCloseButton,
    hideNavigation
);
```

**After (v3.0.0):**
```kotlin
// Builder pattern with comprehensive options
val profileRequest = ShowProfileRequest.builder()
    .customerId("customer-123")
    .showCloseButton(true)
    .hideNavigation(false)
    .closeButtonColor("#FF0000")
    .openDetail("achievements")
    .build()

GameballApp.getInstance(this).showProfile(activity, profileRequest)
```

### 7. Push Notifications

**Before (v2.x):**
```java
// Handled internally or through separate methods
GameballApp.getInstance(this).setDeviceToken(token);
GameballApp.getInstance(this).setPushProvider("FCM");
```

**After (v3.0.0):**
```kotlin
// Integrated into initialization request
val customerRequest = InitializeCustomerRequest.builder()
    .customerId("customer-123")
    .deviceToken("fcm-device-token")
    .pushProvider(PushProvider.FCM) // or PushProvider.HMS
    .build()
```

---

## Common Migration Patterns

### Error Handling Migration

**Before (v2.x):**
```java
// Limited error information
try {
    GameballApp.getInstance(this).registerCustomer(...);
} catch (Exception e) {
    Log.e("Error", e.getMessage());
}
```

**After (v3.0.0):**
```kotlin
// Comprehensive error handling with specific types
GameballApp.getInstance(this).initializeCustomer(
    customerRequest,
    object : Callback<InitializeCustomerResponse> {
        override fun onError(error: Throwable) {
            when (error) {
                is IllegalArgumentException -> {
                    // Invalid input data
                    Log.e("Gameball", "Invalid input: ${error.message}")
                }
                is IllegalStateException -> {
                    // SDK not properly initialized
                    Log.e("Gameball", "SDK error: ${error.message}")
                }
                else -> {
                    // Network or other errors
                    Log.e("Gameball", "Network error: ${error.message}")
                }
            }
        }
    }
)
```

### Language Migration (Java → Kotlin)

If you were using Java:

**Before (v2.x - Java):**
```java
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GameballApp.getInstance(this).registerCustomer(
            "customer-123",
            "customer@example.com",
            "+1234567890",
            null,
            new Callback<CustomerRegisterResponse>() {
                @Override
                public void onSuccess(CustomerRegisterResponse response) {
                    // Handle success
                }

                @Override
                public void onError(Throwable error) {
                    // Handle error
                }
            }
        );
    }
}
```

**After (v3.0.0 - Kotlin):**
```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val customerRequest = InitializeCustomerRequest.builder()
            .customerId("customer-123")
            .email("customer@example.com")
            .mobileNumber("+1234567890")
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
    }
}
```

---

## Migration Checklist

### Pre-Migration
- [ ] Review current Gameball SDK usage in your app
- [ ] Identify all places where SDK methods are called
- [ ] Plan for testing after migration
- [ ] Backup your current implementation

### During Migration
- [ ] Add required Maven repositories (Huawei and JitPack)
- [ ] Update Gradle dependency to v3.0.0
- [ ] Convert initialization code to use `GameballConfig`
- [ ] Replace `registerCustomer` calls with `initializeCustomer`
- [ ] Update customer attributes to use builder pattern
- [ ] Migrate event tracking to new `Event` builder
- [ ] Update profile widget calls to use `ShowProfileRequest`
- [ ] Update push notification handling
- [ ] Convert Java code to Kotlin (if applicable)

### Post-Migration
- [ ] Test all SDK functionality
- [ ] Verify error handling works correctly
- [ ] Test push notifications
- [ ] Verify profile widget displays correctly
- [ ] Test event tracking
- [ ] Run full integration tests

---

## Troubleshooting

### Common Issues

1. **Build Errors After Update**
   ```
   Error: Unresolved reference: registerCustomer
   ```
   **Solution**: Replace with `initializeCustomer` and use builder pattern.

2. **Type Mismatch Errors**
   ```
   Error: Type mismatch. Required: InitializeCustomerRequest
   ```
   **Solution**: Use the builder pattern to create request objects.

3. **Missing Required Fields**
   ```
   Error: Customer ID cannot be empty
   ```
   **Solution**: Ensure all required fields are provided through the builder.

### Getting Help

If you encounter issues during migration:

1. **Check the Examples**: Refer to the code examples in this guide
2. **Review the README**: See [README.md](README.md) for complete usage examples
3. **Check the Changelog**: See [CHANGELOG.md](CHANGELOG.md) for all changes
4. **Developer Documentation**: Visit [https://developer.gameball.co/](https://developer.gameball.co/)
5. **Contact Support**: Email support@gameball.co for assistance

---

## Benefits After Migration

After completing the migration, you'll benefit from:

✅ **Better Type Safety**: Kotlin's null safety and type system prevent runtime errors

✅ **Improved Developer Experience**: Builder pattern with IDE auto-completion

✅ **Better Error Handling**: Specific error types and proper callback mechanisms

✅ **Enhanced Performance**: Optimized internal architecture with reduced overhead

✅ **Future-Proof**: Modern architecture ready for future enhancements

✅ **Consistent API**: Unified design patterns across all SDK methods

---

*For additional help with migration, please visit our [Developer Documentation](https://developer.gameball.co/) or contact our support team at support@gameball.co*