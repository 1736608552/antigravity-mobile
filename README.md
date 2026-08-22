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
app/src/main/java/com/antigravity/mobile/
├── AntigravityApp.kt        # Application class
├── network/
│   └── TokenManager.kt      # Token & Server preferences
└── ui/
    ├── MainActivity.kt      # Edge-to-edge WebView & system bar adapter
    └── LoginActivity.kt     # Web authentication host
```

## GitHub Actions CI/CD

The repository includes an automatic build & release workflow (`.github/workflows/build.yml`) that:
1. Builds signed debug and release APKs upon pushing any `v*` tag
2. Creates a GitHub Release and uploads downloadable APK artifacts automatically

## License

This project is licensed under the MIT License.
