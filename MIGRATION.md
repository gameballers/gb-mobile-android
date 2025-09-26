# Migration Guide: Gameball Android SDK v2.x → v3.0.0

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
4. **Contact Support**: Email support@gameball.co for assistance

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

*For additional help with migration, please contact our support team at support@gameball.co*