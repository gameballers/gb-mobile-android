# 📋 Changelog

All notable changes to Gameball Android SDK are documented here

## [3.2.0] - 2026-06-17 📱

> **Minor Release**: Widget event channel, widget dismissal controls, external-link handling, diagnostic logging, and channel-merging parameters

### ✨ Added
- 🏗️ **Widget Event Channel**: `ShowProfileRequest.widgetEventCallback` receives events posted from the widget (e.g. game completion) as a `Map<String, Object>` `{type, metadata}`; the `gameCompleted` payload carries `hasWon`, `rewardType`, `discountType`, `rewardName`, `campaignId`, `campaignType`
- 🏗️ **Web-Initiated Close**: the widget can dismiss its own webview via `window.GameballWidget.closeWidget()`
- 🏗️ **Host-Initiated Dismiss**: new `GameballApp.hideProfile()` dismisses the widget programmatically (no-op when nothing is shown)
- ⚙️ **External-Link Handling**: links flagged `gbExternalBrowser=true` open in the system browser; optional `externalLinkCallback` lets the host intercept them
- 📊 **Diagnostic Logging**: added internal diagnostic logging to aid SDK troubleshooting
- 📇 **Channel-Merging Parameters**: `ShowProfileRequest.builder()` now accepts optional `mobile` and `email` to support customer channel merging

### 🔄 Changed
- 🔧 **User-Agent Header**: unified the `x-gb-agent` header format to `GB/<sdkType>/<version>`


## [3.1.2] - 2025-12-15 🔧

> **Patch Release**: Widget URL fix

### 🐛 Fixed
- 🔧 **Widget URL**: eliminated a null `customerId` value from the widget URL

## [3.1.1] - 2025-12-15 🔧

> **Patch Release**: Guest mode support for profile widget

### 🐛 Fixed
- 🎁 **Guest Mode Support**: Profile widget can now be displayed without customer authentication
- 🔓 **Optional Customer ID**: `ShowProfileRequest.customerId` is now optional, defaulting to `null` for guest mode

### 🔄 Changed
- 🏗️ **ShowProfileRequest Builder**: No longer requires customer ID - supports guest mode scenarios
- 📝 **Widget URL Construction**: Enhanced to support both authenticated and guest modes

### 🛠️ Developer Experience
- ⚡ **Simpler API**: Build `ShowProfileRequest` without customer ID for guest mode
- 🎯 **Flexible Usage**: Support for preview/showcase scenarios before user registration
- 📖 **Better Documentation**: Clear examples for both guest and authenticated modes

---

## [3.1.0] - 2025-10-14 🔒

> **Security Release**: Token-based authentication with per-request override support

### 🔒 Security
- 🛡️ Added Session Token authentication mechanism for secure API communication
- 🔐 Optional `sessionToken` parameter in `GameballConfig` for global token-based authentication
- 🎯 Per-request session token override support for flexible authentication control
- 🔄 Automatic secure endpoint routing (API v4.0 → v4.1) when session token is provided
- 📡 `X-GB-TOKEN` header added to requests when using session token authentication

### ✨ Added
- 🎯 Optional `sessionToken` parameter added to `initializeCustomer()` method
- 🎯 Optional `sessionToken` parameter added to `sendEvent()` method
- 🎯 Optional `sessionToken` parameter added to `showProfile()` method
- 🔄 Per-request token override allows temporary authentication changes without affecting global state
- ♻️ Passing `null` as sessionToken clears the token for that specific request

### 🔧 Internal Changes
- 💾 Added SharedPreferences support for secure token storage and management
- 🏷️ Standardized internal widget parameters (`playerid` → `customerId`)
- 📊 Added API version constants (`API_V4_0`, `API_V4_1`) for version management
- 🎨 Added `@JvmOverloads` annotation for backward compatibility with existing code

---

## [3.0.0] - 2025-09-29 🎉

> **Major Release**: Complete Kotlin rewrite with modern architecture

### ✨ Added
- 🏗️ Complete SDK rewrite with modern Kotlin architecture and builder pattern
- ⚙️ New `GameballConfig` class for centralized SDK configuration
- 🔧 `InitializeCustomerRequest` with comprehensive builder pattern and validation
- 👤 Enhanced `CustomerAttributes` model with flexible custom attributes support
- 📊 Modern `Event` tracking system with metadata support
- 🎨 `ShowProfileRequest` for profile widget customization
- ⚡ Kotlin coroutines support for better async operations
- ✅ Comprehensive input validation with proper error callbacks
- 📱 Support for both Firebase Cloud Messaging (FCM) and Huawei Push Kit (HMS)
- 🛡️ Enhanced type safety with non-nullable Kotlin types
- 🚨 Improved error handling with specific exception types

### 🔄 Changed
- 💥 **BREAKING**: Migrated entire codebase from Java to Kotlin
- 💥 **BREAKING**: Replaced `registerCustomer()` with `initializeCustomer()` using builder pattern
- 💥 **BREAKING**: Updated method signatures to use builder pattern for all requests
- 💥 **BREAKING**: Changed `sendEvent()` to use new `Event` builder model
- 💥 **BREAKING**: Updated `showProfile()` to use `ShowProfileRequest` builder
- 💥 **BREAKING**: Renamed service method from `registerDevice` to `initializeCustomerService`
- 🔧 Enhanced validation logic with better error messages and proper callback handling
- 🚀 Improved SDK initialization flow with centralized configuration
- 🏗️ Streamlined internal architecture by eliminating duplicate request creation
- 📦 Updated dependency management with modern Android libraries

### 🗑️ Removed
- 💥 **BREAKING**: Removed legacy Java-based request models (`CustomerRegisterRequest`)
- 💥 **BREAKING**: Removed deprecated Firebase Dynamic Links functionality
- 💥 **BREAKING**: Removed multiple method overloads in favor of builder pattern
- 💥 **BREAKING**: Removed direct parameter-based initialization methods
- 💥 **BREAKING**: Removed legacy `registerCustomer` method variants
- 🧹 Removed unused internal properties and simplified state management
- 🧹 Removed redundant validation layers and error handling paths

### 🐛 Fixed
- 🔧 Fixed data duplication issues in customer initialization flow
- 🔧 Resolved potential data loss when processing customer requests
- 🔧 Fixed SharedPreferencesUtils null pointer crash on activity launch
- 🔧 Corrected error handling to properly notify callers instead of silent failures
- 🔧 Enhanced input validation to prevent invalid data from reaching API endpoints
- 🔧 Improved memory management by removing unnecessary object allocations
- 🔧 Fixed widget profile long press selection behavior

### 🔒 Security
- 🛡️ Enhanced request validation to prevent malformed data submission
- 🛡️ Improved error handling to avoid exposing sensitive information in logs
- 🛡️ Added proper input sanitization for all user-provided data

---

## [2.3.0] - 2025-08-27 📱

### ✨ Added
- 🔄 Delegated device token setting to initialize customer flow
- 📲 Enhanced push notification handling

### 🔄 Changed
- ⚡ Improved device token management workflow

### 🐛 Fixed
- 🔧 Fixed device token initialization timing issues

---

## [2.2.0] - 2025-03-30 ⚠️

### ✨ Added
- ⚠️ Added deprecation warnings for Firebase Dynamic Links dependent methods
- 🔗 Enhanced referral link auto-detection functionality

### 🔄 Changed
- 🏗️ Extracted Firebase referral link detection into separate method
- 📦 Improved method organization and separation of concerns

### 📛 Deprecated
- ⚠️ Deprecated `registerCustomer` method overrides that depend on Firebase Dynamic Links

---

## [2.1.0] - 2025-03-06 🔧

### 🐛 Fixed
- 🔧 Fixed SharedPreferencesUtils null pointer crash on activity launch
- 🛡️ Converted SharedPreferencesUtils to Kotlin singleton with safe initialization
- ✅ Added comprehensive null checks to prevent runtime crashes

---

## [2.0.0] - 2024-12-11 🎯

### ✨ Added
- 🏗️ Major architectural improvements
- 🚀 Enhanced SDK stability and performance

### 🔄 Changed
- 💥 **BREAKING**: Updated core SDK architecture
- ⚡ Improved overall SDK reliability

## [1.5.1] - 2024-10-22

### Fixed
- Minor bug fixes and stability improvements

## [1.5.0] - 2024-10-15

### Added
- New features and enhancements
- Improved SDK functionality

## [1.4.1] - 2024-12-02

### Fixed
- Bug fixes and stability improvements

## [1.4.0] - 2024-08-01

### Added
- Enhanced SDK capabilities
- New feature additions

## [1.3.0] - 2023-12-31

### Added
- Enhanced SDK features and capabilities

## [1.2.10] - 2023-10-03

### Fixed
- Bug fixes and improvements

## [1.2.9] - 2023-09-20

### Fixed
- Minor fixes and enhancements

## [1.2.8] - 2023-09-18

### Fixed
- Bug fixes and stability improvements

## [1.2.7] - 2023-09-13

### Fixed
- Minor bug fixes and improvements

## [1.2.6] - 2023-09-05

### Fixed
- Bug fixes and stability improvements

## [1.2.5] - 2023-08-24

### Fixed
- Minor fixes and enhancements

## [1.2.4] - 2023-08-22

### Fixed
- Bug fixes and improvements

## [1.2.3] - 2023-08-11

### Fixed
- Stability improvements and bug fixes

## [1.2.2] - 2023-08-06

### Fixed
- Minor bug fixes

## [1.2.1] - 2023-07-30

### Fixed
- Bug fixes and improvements

## [1.2.0] - 2023-05-07

### Added
- New features and enhancements

### Fixed
- Various bug fixes and improvements

## [1.1.2] - 2023-03-14

### Fixed
- Bug fixes and stability improvements

## [1.1.1] - 2021-05-17

### Fixed
- Minor bug fixes

## [1.1.0] - 2021-05-05

### Added
- New functionality and features

### Fixed
- Bug fixes and improvements

## [1.0.1] - 2021-03-31

### Fixed
- Initial release bug fixes

## [1.0.0] - 2021-03-30

### Added
- Initial stable release of Gameball Android SDK
- Customer registration and management
- Event tracking functionality
- Profile widget support
- Push notification integration