# Google Antigravity Mobile

An Android client for [Google Antigravity WebUI](http://fn0.xx.kg:3100).

Connect to your self-hosted Google Antigravity server directly from your Android device.

## Features

- **WebView Wrapper** - Native Android shell around the Antigravity Web UI
- **Pull to Refresh (下拉刷新)** - Swipe down to reload the current page seamlessly
- **Google Antigravity Brand** - Official Antigravity theme, icons, and colors
- **File Upload** - Full support for file and image uploads through the web interface
- **Dark/Light Theme** - Sleek Google Antigravity dark theme
- **Cookie & Token Persistence** - Stay logged in across app restarts
- **Edge-to-Edge Experience** - Dynamic status bar and navigation bar integration

## Quick Start

### Build from Source

```bash
# Clone the repository
git clone https://github.com/djsevenx1/antigravity-mobile.git
cd antigravity-mobile

# Build debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Configuration

默认直接连接到 `http://fn0.xx.kg:3100`，也可在登录页随时输入/切换其它 Antigravity 部署节点。
npm install -g @cloudcli-ai/cloudcli
cloudcli
```

Then connect from the app using your machine's IP address and port 3001.

## Tech Stack

- **Kotlin** - 100% Kotlin
- **AndroidX** - Core libraries
- **Material Design 3** - UI components
- **WebView** - Web content rendering
- **SQLite** - Server configuration storage
- **ViewBinding** - Type-safe view access
- **GitHub Actions** - CI/CD for APK builds

## Project Structure

```
app/src/main/java/com/claudecode/mobile/
├── ClaudeCodeApp.kt          # Application class
├── MainActivity.kt           # WebView host activity
├── SettingsActivity.kt       # Server management
├── AddServerActivity.kt      # Add/edit server form
├── data/
│   ├── ServerConfig.kt       # Server data model
│   └── ServerRepository.kt   # SQLite storage
└── web/
    └── CloudWebViewClient.kt # WebView client with error handling
```

## Building

### Prerequisites

- Android Studio Hedgehog (2023.1) or later
- JDK 17
- Android SDK 34

### Release Build

```bash
# Generate a signing keystore
keytool -genkey -v -keystore release.keystore -alias claudecode \
  -keyalg RSA -keysize 2048 -validity 10000

# Build release APK
./gradlew assembleRelease

# Sign the APK
apksigner sign --ks release.keystore \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

### GitHub Actions

The repository includes a GitHub Actions workflow (`.github/workflows/build.yml`) that:
1. Builds debug and release APKs on tag push
2. Signs the release APK (if signing secrets are configured)
3. Creates a GitHub Release with the APK attached

To configure release signing, add these repository secrets:
- `SIGNING_KEY` - Base64-encoded keystore file
- `KEY_ALIAS` - Keystore alias
- `KEY_STORE_PASSWORD` - Keystore password
- `KEY_PASSWORD` - Key password

## License

This project is licensed under the MIT License.

## Acknowledgments

- [Claude Code UI](https://github.com/siteboon/claudecodeui) by siteboon
- [Claude Code](https://docs.anthropic.com/en/docs/claude-code) by Anthropic
