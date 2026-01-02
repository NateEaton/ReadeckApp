# ReadeckApp Development Setup Guide

This guide covers setting up a development environment for the ReadeckApp Android/Kotlin project.

## Prerequisites

- Basic familiarity with command line
- Git installed
- An Android device for testing (optional but recommended)

---

## Option 1: MacBook Pro (Recommended)

The best experience for Android development with full IDE support.

### Install Android Studio

**Via Homebrew (easiest):**
```bash
brew install --cask android-studio
```

**Manual download:**
1. Go to https://developer.android.com/studio
2. Download the macOS version (~1GB)
3. Drag to Applications folder

### First-time Setup

1. Launch Android Studio
2. Follow the Setup Wizard:
   - Choose "Standard" installation
   - Accept all SDK license agreements
   - Wait for SDK components to download (~5-10 min)

3. Open the project:
   - File → Open → Select the `ReadeckApp` folder
   - Wait for Gradle sync to complete (first time takes several minutes)

### Running the App

**Using Emulator:**
1. Tools → Device Manager
2. Click "Create Device"
3. Select a Pixel device → Next
4. Download a system image (API 34 recommended) → Next → Finish
5. Click the play button on your new virtual device
6. Click the green Run button (▶) in the toolbar

**Using Physical Device:**
1. Enable Developer Options on your Android phone:
   - Settings → About Phone → Tap "Build Number" 7 times
2. Enable USB Debugging:
   - Settings → Developer Options → USB Debugging → On
3. Connect via USB cable
4. Accept the debugging prompt on your phone
5. Select your device in the toolbar dropdown
6. Click Run (▶)

### Useful Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run all tests
./gradlew test

# Clean build
./gradlew clean

# Check for lint issues
./gradlew lint
```

### System Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| RAM | 8GB | 16GB+ |
| Disk Space | 15GB | 30GB+ |
| macOS | 10.14+ | Latest |

---

## Option 2: Pixel Slate (Linux/Crostini)

Android Studio runs on Linux but with limitations on Chrome OS devices.

### Install Android Studio

```bash
# Update package list
sudo apt update

# Install required libraries
sudo apt install -y libc6 libncurses5 libstdc++6 lib32z1 libbz2-1.0

# Download Android Studio
cd ~/Downloads
wget https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2024.2.1.11/android-studio-2024.2.1.11-linux.tar.gz

# Extract
sudo tar -xzf android-studio-*.tar.gz -C /opt/

# Create launcher script
echo 'export PATH=$PATH:/opt/android-studio/bin' >> ~/.bashrc
source ~/.bashrc

# Launch
studio.sh
```

### First-time Setup

1. Follow the Setup Wizard (same as macOS)
2. When prompted for SDK location, use: `~/Android/Sdk`
3. Skip emulator setup (won't work on Pixel Slate)

### Important Limitations

| Feature | Status |
|---------|--------|
| Code editing | ✅ Works |
| Building APK | ✅ Works |
| Emulator | ❌ Not supported (ARM + no nested virtualization) |
| USB debugging | ⚠️ May require extra setup |

### Testing on Physical Device

Since emulators won't work, you'll need a physical Android device:

1. Enable USB debugging on your Android phone
2. Connect via USB-C hub
3. You may need to configure udev rules:

```bash
# Create udev rules for Android devices
sudo bash -c 'cat > /etc/udev/rules.d/51-android.rules << EOF
SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="04e8", MODE="0666", GROUP="plugdev"
EOF'

sudo udevadm control --reload-rules
sudo usermod -aG plugdev $USER
```

4. Reconnect device and accept debugging prompt

### Wireless ADB (Alternative)

If USB is problematic:

```bash
# On the Android device, enable Wireless Debugging
# Settings → Developer Options → Wireless Debugging

# Pair (one-time)
adb pair <ip>:<pairing-port>
# Enter pairing code shown on device

# Connect
adb connect <ip>:<port>
```

---

## Option 3: Code-Server on Synology NAS

A lightweight option for code editing and CLI builds. No emulator support.

### Container Setup

Create a Docker container with the required tools. Add this to your `docker-compose.yml`:

```yaml
version: '3.8'
services:
  android-dev:
    image: ubuntu:22.04
    container_name: android-dev
    volumes:
      - /volume1/docker/android-dev/workspace:/workspace
      - /volume1/docker/android-dev/android-sdk:/opt/android-sdk
    environment:
      - ANDROID_HOME=/opt/android-sdk
      - JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
    ports:
      - "8443:8443"  # code-server
    command: /bin/bash -c "while true; do sleep 1000; done"
```

### Install Dependencies

SSH into the container and run:

```bash
# Update and install base packages
apt update && apt install -y \
    openjdk-17-jdk \
    wget \
    unzip \
    git \
    curl

# Set JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$PATH:$JAVA_HOME/bin' >> ~/.bashrc

# Download Android command-line tools
mkdir -p /opt/android-sdk/cmdline-tools
cd /opt/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-*.zip
mv cmdline-tools latest

# Set Android environment variables
echo 'export ANDROID_HOME=/opt/android-sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc

# Accept licenses
yes | sdkmanager --licenses

# Install required SDK components
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

### Install code-server

```bash
# Install code-server
curl -fsSL https://code-server.dev/install.sh | sh

# Configure
mkdir -p ~/.config/code-server
cat > ~/.config/code-server/config.yaml << EOF
bind-addr: 0.0.0.0:8443
auth: password
password: your-secure-password
cert: false
EOF

# Start code-server
code-server --config ~/.config/code-server/config.yaml
```

### VS Code Extensions for Kotlin

Install these extensions in code-server for better Kotlin support:

1. **Kotlin Language** (`mathiasfrohlich.Kotlin`) - Syntax highlighting
2. **Kotlin** (`fwcd.kotlin`) - Language server (requires JDK)

Note: IntelliJ-level features (refactoring, full autocomplete) are limited in VS Code.

### Building the Project

```bash
# Clone the repo
cd /workspace
git clone <repository-url> ReadeckApp
cd ReadeckApp

# Make gradlew executable
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

### Transferring APK to Device

**Option A: Via Synology File Station**
1. Access the APK via File Station
2. Download to your phone
3. Install (enable "Install from unknown sources")

**Option B: Via ADB over network**
```bash
# On your Android device, enable Wireless Debugging
# Get the IP and port from Developer Options

adb connect <phone-ip>:<port>
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Limitations

| Feature | Status |
|---------|--------|
| Code editing | ✅ Works |
| Syntax highlighting | ✅ Works |
| Building APK | ✅ Works |
| Full autocomplete | ⚠️ Limited |
| Refactoring tools | ❌ Not available |
| Emulator | ❌ Not available |
| Layout preview | ❌ Not available |

---

## Recommended Workflow

For developers new to Kotlin/Android:

1. **Start with Android Studio on Mac** - Learn the codebase with full IDE support
2. **Use code-server for quick edits** - Once familiar, use for minor changes
3. **Keep a physical Android device** - Essential for testing without emulator

---

## Troubleshooting

### Gradle sync fails
```bash
# Clear Gradle cache
rm -rf ~/.gradle/caches
./gradlew clean
```

### SDK not found
```bash
# Verify ANDROID_HOME is set
echo $ANDROID_HOME

# Should output something like:
# /Users/<name>/Library/Android/sdk (Mac)
# /opt/android-sdk (Linux/Container)
```

### Build fails with memory error
```bash
# Increase Gradle memory in gradle.properties
echo "org.gradle.jvmargs=-Xmx4096m" >> gradle.properties
```

### Device not detected
```bash
# Check ADB sees the device
adb devices

# Should show your device serial number
# If empty, check USB debugging is enabled
```

---

## Project Structure Quick Reference

```
ReadeckApp/
├── app/
│   ├── src/main/java/de/readeckapp/
│   │   ├── domain/          # Business logic, models
│   │   ├── io/              # Database, API, preferences
│   │   ├── ui/              # Compose UI screens
│   │   └── worker/          # Background sync
│   ├── src/main/res/        # Resources (strings, icons)
│   └── build.gradle.kts     # App-level build config
├── gradle/                   # Gradle wrapper
├── build.gradle.kts          # Project-level build config
└── settings.gradle.kts       # Project settings
```

---

## Useful Resources

- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose Basics](https://developer.android.com/jetpack/compose)
- [Android Developer Guides](https://developer.android.com/guide)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
