# Technical Spec: GitHub Actions for Android Builds

**Document Type:** Setup & Operations Guide
**Purpose:** Enable remote builds without Android Studio
**Date:** 2026-01-25
**Status:** Implementation guide for MyDeck

---

## Overview

GitHub Actions workflows automate Android app builds, allowing you to create signed APKs for testing without needing Android Studio. You can trigger builds from any device and download APKs directly to your phone.

**Key Benefits:**
- Build from anywhere (no MBP required)
- Automatic builds on branch pushes
- Manual on-demand builds
- Signed APKs ready for installation
- Build artifacts stored in GitHub

---

## Existing Workflow Files

The repository already includes these workflows in `.github/workflows/`:

| Workflow File | Trigger | Purpose | Output |
|---------------|---------|---------|--------|
| `test-build.yml` | Manual | On-demand test builds | APK artifact (5-day retention) |
| `dev-release.yml` | Auto + Manual | Snapshot builds from develop | GitHub Release + APK |
| `release.yml` | Tag push | Official releases | Draft GitHub Release + APK |
| `build.yml` | Push/PR | CI build checks | Build verification only |
| `run-tests.yml` | Push/PR | Unit tests | Test results |
| `kotlin-syntax-check.yml` | Push/PR | Syntax validation | Lint results |

---

## Part 1: Initial Setup

### Step 1: Generate App Signing Keystore

**On your MBP (one-time setup):**

```bash
# Navigate to project directory
cd ~/path/to/ReadeckApp

# Generate release keystore
keytool -genkey -v \
  -keystore readeck-release.keystore \
  -alias readeck \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# You will be prompted for:
# - Keystore password (remember this!)
# - Key password (can be same as keystore password)
# - Your name and organization details
```

**IMPORTANT:** Save these passwords securely (password manager)!

**Convert keystore to base64 for GitHub:**

```bash
# macOS/Linux
base64 -i readeck-release.keystore -o keystore-base64.txt

# This creates a text file with base64-encoded keystore
# You'll upload this to GitHub secrets
```

### Step 2: Configure GitHub Secrets

**Navigate to GitHub Repository Settings:**
1. Go to `https://github.com/YourUsername/ReadeckApp`
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**

**Add these 4 secrets:**

| Secret Name | Value | Description |
|-------------|-------|-------------|
| `SIGNING_KEY` | Contents of `keystore-base64.txt` | Base64-encoded keystore file |
| `ALIAS` | `readeck` (or your chosen alias) | Key alias from keytool command |
| `KEY_STORE_PASSWORD` | Your keystore password | Password for the keystore file |
| `KEY_PASSWORD` | Your key password | Password for the key (often same as keystore) |

**Security Notes:**
- Never commit keystore files to git
- Never share these secrets
- Keep backup of keystore file in secure location
- If keystore is lost, you cannot update published apps!

### Step 3: Verify Secrets

**Check that all secrets are set:**
1. Go to **Settings** → **Secrets and variables** → **Actions**
2. Verify you see these 4 secrets listed:
   - `SIGNING_KEY`
   - `ALIAS`
   - `KEY_STORE_PASSWORD`
   - `KEY_PASSWORD`

---

## Part 2: Building APKs

### Option A: Manual Test Build (Recommended for Testing)

**Use Case:** Quick test build from current branch

**Steps:**
1. Go to **Actions** tab on GitHub
2. Click **Build Test Release** in left sidebar
3. Click **Run workflow** button (top right)
4. Select branch to build from
5. Click green **Run workflow** button

**Build Time:** ~5-10 minutes

**Output:**
- APK artifact available for 5 days
- File name: `test-release-{commit-sha}.apk`
- Includes SHA256 checksum file

**Download APK:**
1. Wait for workflow to complete (green checkmark)
2. Click on the completed workflow run
3. Scroll to **Artifacts** section at bottom
4. Click artifact name to download ZIP
5. Extract ZIP to get APK file

### Option B: Automatic Snapshot Build (Develop Branch)

**Use Case:** Regular test builds from develop branch

**Trigger:** Automatically runs on every push to `develop` branch

**Also available manually:**
1. Go to **Actions** tab
2. Click **Build Snapshot Release**
3. Click **Run workflow**
4. Select `develop` branch
5. Click **Run workflow**

**Output:**
- Creates/updates GitHub Release: `develop-snapshot`
- Pre-release tagged as "Snapshot Build"
- APK with timestamp version name: `YYYYMMDDTHHMMSS-{commit-sha}`
- Includes checksums

**Download APK:**
1. Go to **Releases** page (right sidebar on main repo page)
2. Click **develop-snapshot** release
3. Scroll to **Assets** section
4. Click APK file to download directly

### Option C: Official Release Build

**Use Case:** Production releases with version numbers

**Trigger:** Push a version tag

**Steps:**
1. Update version in `app/build.gradle.kts`:
   ```kotlin
   versionCode = 900
   versionName = "0.9.0"
   ```
2. Commit and push changes
3. Create and push git tag:
   ```bash
   git tag v0.9.0
   git push origin v0.9.0
   ```

**Output:**
- Creates Draft GitHub Release: `Release v0.9.0`
- APK attached to release
- Allows editing release notes before publishing

**Download APK:**
1. Go to **Releases** page
2. Review draft release
3. Edit release notes if needed
4. Download APK from **Assets**
5. Publish release when ready

---

## Part 3: Installing APKs on Phone

### Method 1: Direct Download (Easiest)

**From phone browser:**
1. Open `https://github.com/YourUsername/ReadeckApp/releases`
2. Tap release (e.g., `develop-snapshot`)
3. Tap APK file in Assets section
4. Confirm download
5. Open downloaded APK file
6. Android will prompt: "Install unknown apps"
7. Tap **Settings** → Enable **Allow from this source**
8. Tap **Install**

### Method 2: Download Then Transfer

**From computer:**
1. Download APK from GitHub Releases or Actions Artifacts
2. Transfer to phone:
   - **USB:** Copy to phone storage via cable
   - **Cloud:** Upload to Google Drive/Dropbox, download on phone
   - **ADB:** `adb install path/to/app.apk`

**On phone:**
1. Open file manager
2. Navigate to downloaded APK
3. Tap APK file
4. Follow installation prompts

### Method 3: Using ADB (Technical)

**Prerequisites:**
- USB debugging enabled on phone
- ADB installed on computer

**Steps:**
```bash
# Connect phone via USB
# Verify connection
adb devices

# Install APK
adb install -r path/to/ReadeckApp-0.8.0.apk

# The -r flag reinstalls/replaces existing app
```

---

## Part 4: Workflow Details

### Test Build Workflow (`test-build.yml`)

**What it does:**
1. Checks out code from selected branch
2. Sets up JDK 21, Gradle, Android SDK
3. Decodes keystore from GitHub secrets
4. Builds `githubSnapshotRelease` flavor
5. Creates SHA256 checksum
6. Uploads APK as artifact (5-day retention)

**Gradle Command:**
```bash
./gradlew assembleGithubSnapshotRelease
```

**Environment Variables:**
- `SNAPSHOT_VERSION_NAME`: Timestamp + commit SHA
- `KEY_ALIAS`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD`: From secrets
- `KEYSTORE`: Path to decoded keystore

**APK Output:**
- Path: `app/build/outputs/apk/githubSnapshot/release/`
- Name: `ReadeckApp-{version}.apk`

### Snapshot Build Workflow (`dev-release.yml`)

**What it does:**
1. Same build steps as test build
2. Creates/updates git tag: `develop-snapshot`
3. Creates/updates GitHub pre-release
4. Attaches APK and checksums to release
5. Removes previous artifacts (keeps only latest)

**Differences from Test Build:**
- Creates persistent GitHub Release
- No expiration on artifacts
- Auto-updates on every develop push
- Includes release notes

### Release Build Workflow (`release.yml`)

**What it does:**
1. Triggered by version tag (e.g., `v0.9.0`)
2. Builds `githubReleaseRelease` flavor
3. Uses version from tag
4. Creates draft GitHub Release
5. Requires manual publishing

**Gradle Command:**
```bash
./gradlew assembleGithubReleaseRelease
```

**Release Settings:**
- Draft: `true` (requires manual publish)
- Prerelease: `true` (marks as pre-release)
- Allows editing release notes before publishing

---

## Part 5: Product Flavors

**Configured in `app/build.gradle.kts`:**

### githubSnapshot Flavor

```kotlin
create("githubSnapshot") {
    dimension = "version"
    applicationIdSuffix = ".snapshot"
    versionName = System.getenv()["SNAPSHOT_VERSION_NAME"]
        ?: "${defaultConfig.versionName}-snapshot"
    versionCode = System.getenv()["SNAPSHOT_VERSION_CODE"]?.toInt()
        ?: defaultConfig.versionCode
    signingConfig = signingConfigs.getByName("release")
}
```

**Characteristics:**
- App ID: `de.readeckapp.snapshot`
- Can install alongside release version
- Version name: Timestamp or custom
- Signed with release keystore

### githubRelease Flavor

```kotlin
create("githubRelease") {
    dimension = "version"
    versionName = System.getenv()["RELEASE_VERSION_NAME"]
        ?: defaultConfig.versionName
    versionCode = System.getenv()["RELEASE_VERSION_CODE"]?.toInt()
        ?: defaultConfig.versionCode
    signingConfig = signingConfigs.getByName("release")
}
```

**Characteristics:**
- App ID: `de.readeckapp` (production)
- Version from git tag or default
- Signed with release keystore
- For official releases

---

## Part 6: Troubleshooting

### Build Fails: "No value has been specified for property 'signingConfig.storeFile'"

**Cause:** Keystore file not found

**Fix:**
1. Verify `SIGNING_KEY` secret exists in GitHub
2. Check secret value is valid base64
3. Re-generate base64 if needed:
   ```bash
   base64 -i readeck-release.keystore -o keystore-base64.txt
   ```

### Build Fails: "Given final block not properly padded"

**Cause:** Invalid base64 encoding

**Fix:**
1. Ensure base64 command used `-i` and `-o` flags
2. Copy entire base64 output (no truncation)
3. Remove any extra whitespace/newlines
4. Re-upload to GitHub secrets

### Build Succeeds but APK Won't Install

**Cause:** Signature mismatch with existing app

**Fix:**
1. Uninstall existing app first
2. Or use different flavor (snapshot vs release)
3. Or use `adb install -r` to force reinstall

### Can't Find Artifacts After Build

**Cause:** Artifact retention expired or wrong workflow

**Fix:**
- Test builds: 5-day retention, check within 5 days
- Snapshot builds: Check Releases page, not Artifacts
- Release builds: Check Releases page for draft

### "App not installed" Error on Phone

**Possible Causes:**
1. **Insufficient storage:** Free up space
2. **Corrupted download:** Re-download APK
3. **Signature conflict:** Uninstall old version first
4. **Architecture mismatch:** Verify APK architecture

**Debug:**
```bash
# Check APK details
aapt dump badging path/to/app.apk | grep package

# Check device architecture
adb shell getprop ro.product.cpu.abi
```

---

## Part 7: Customization for MyDeck

When refactoring to MyDeck, update these files:

### 1. Product Flavor Names (`app/build.gradle.kts`)

```kotlin
productFlavors {
    create("githubSnapshot") {
        dimension = "version"
        applicationIdSuffix = ".snapshot"
        // Keep configuration as-is
    }
    create("githubRelease") {
        dimension = "version"
        applicationId = "com.mydeck.app"  // ← Change this
        // Keep rest of configuration
    }
}
```

### 2. App Name in Release Notes

**Update workflow files:**

`dev-release.yml`:
```yaml
- name: Create release
  uses: ncipollo/release-action@v1.16.0
  with:
    name: MyDeck Snapshot Build  # ← Change this
    body: |
      This release represents a snapshot build from the `develop` branch...
```

`release.yml`:
```yaml
- name: Create Release
  uses: ncipollo/release-action@v1.16.0
  with:
    name: MyDeck Release v${{ steps.get_version.outputs.version }}  # ← Change this
```

### 3. Keystore Generation

Generate new keystore for MyDeck:
```bash
keytool -genkey -v \
  -keystore mydeck-release.keystore \
  -alias mydeck \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Update GitHub secrets with new keystore and alias.

---

## Part 8: Quick Reference

### Trigger Manual Test Build
1. Actions → Build Test Release → Run workflow → Select branch → Run

### Download Snapshot Build
1. Releases → develop-snapshot → Assets → Download APK

### Create Official Release
```bash
git tag v1.0.0
git push origin v1.0.0
```

### Install APK on Phone
1. Settings → Security → Install unknown apps → Chrome → Allow
2. Download APK from GitHub Releases
3. Open downloaded file → Install

### Check Build Status
- Actions tab → Click workflow run → View logs

---

## Part 9: Best Practices

### Version Management

**Snapshot Builds:**
- Automatic timestamp versions: `20260125T143022-abc1234`
- No need to update version in code
- Can install alongside release version

**Release Builds:**
1. Update `versionCode` and `versionName` in `app/build.gradle.kts`
2. Commit changes
3. Create matching git tag: `v{versionName}`
4. Push tag to trigger build

### Testing Workflow

1. **Develop on feature branch** → Push changes
2. **Merge to develop** → Auto-builds snapshot
3. **Download and test** snapshot on phone
4. **Ready for release** → Tag and build release
5. **Final testing** → Test release build before publishing

### Storage Management

- Test builds expire after 5 days (auto-cleanup)
- Snapshot release always updated (only one kept)
- Release builds persist until manually deleted
- Clean up old releases periodically

### Security

- **Never commit** keystore files
- **Backup keystore** securely (cloud + local)
- **Document passwords** in password manager
- **Rotate keystore** if compromised (requires new app ID)

---

## Part 10: Advanced Options

### Custom Version Names

**For test builds:**

Edit `test-build.yml`:
```yaml
env:
  SNAPSHOT_VERSION_NAME: "test-feature-x"  # Custom name
```

### Parallel Flavor Builds

**Build multiple flavors in one workflow:**

```yaml
- name: Build all flavors
  run: |
    ./gradlew assembleGithubSnapshotRelease
    ./gradlew assembleGithubReleaseRelease
```

### Build with Specific Commit

**Checkout specific commit:**
```yaml
- uses: actions/checkout@v4
  with:
    ref: abc1234  # Commit SHA or tag
```

### Email Notifications

**Add to workflow:**
```yaml
- name: Send notification
  if: failure()
  uses: dawidd6/action-send-mail@v3
  with:
    server_address: smtp.gmail.com
    server_port: 465
    username: ${{ secrets.EMAIL_USERNAME }}
    password: ${{ secrets.EMAIL_PASSWORD }}
    subject: Build failed
    to: you@example.com
    from: github-actions@example.com
    body: Build failed for ${{ github.repository }}
```

---

## Summary

**Setup (One-Time):**
1. Generate keystore on MBP
2. Encode to base64
3. Add 4 secrets to GitHub
4. Done!

**Daily Use:**
1. Push to develop → Auto-builds snapshot
2. Or manually trigger test build from any branch
3. Download APK from GitHub
4. Install on phone

**For Releases:**
1. Update version in build.gradle.kts
2. Push version tag
3. Review draft release
4. Publish when ready

**No MBP Required:** All builds happen on GitHub servers!
