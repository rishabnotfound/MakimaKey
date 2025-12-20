<p align="center">
      <img
        src="./logo/logo.png"
        width="200"
        height="200"
      />
    </p>

# <p align="center">MakimaKey</p>

<p align="center">A secure, fully offline, open-source TOTP authenticator for Android.</p>

## Download

<p align="center">
  <img alt="GitHub Downloads" src="https://img.shields.io/github/downloads/rishabnotfound/MakimaKey/total">
</p>

<p align="center">
  <a href="https://github.com/rishabnotfound/MakimaKey/releases/latest">
    <img
      src="https://custom-icon-badges.demolab.com/badge/Download%20APK-blue?style=for-the-badge&logo=download&logoColor=white"
      alt="Download APK"
    />
  </a>
</p>

<p align="center">
  <sub>
    Not sure which build to choose? Download the <b>Universal</b> version.
  </sub>
</p>



## Preview

<p align="center">
    <img width="379" height="849" alt="image" src="https://github.com/user-attachments/assets/a407ffb8-ff61-4a3e-8296-d96b8d0a237d" />
      <img width="376" height="843" alt="image" src="https://github.com/user-attachments/assets/575056f2-ce6f-486d-8040-78b522e5f001" />
</p

## Features

### Security First
- **Fully Offline** - No internet permission, all data stays on your device
- **Hardware-Backed Encryption** - Uses Android Keystore with AES-256-GCM
- **EncryptedSharedPreferences** - Secrets are encrypted at rest
- **RFC 6238 Compliant** - Proper TOTP implementation with dynamic truncation
- **Screenshot Protection** - Prevents screenshots and screen recording
- **App Lock** - PIN and biometric authentication
- **Auto-Lock** - Automatically locks after 30 seconds in background
- **Secure Clipboard** - Auto-clears OTP codes after 30 seconds

### Features
- Add accounts via QR code scan or manual entry
- Support for SHA-1, SHA-256, SHA-512 algorithms
- Support for 6 and 8 digit codes
- Configurable time periods (default 30s)
- Search accounts
- Reorder accounts
- Encrypted local backups (password-protected)
- Material 3 UI with true black theme (#000000)

## Installation

### Requirements
- Android 8.0 (API 26) or higher
- Camera permission (for QR code scanning)

### Building from Source

1. Clone the repository
```bash
git clone https://github.com/rishabnotfound/MakimaKey.git
cd MakimaKey
```

2. Build with Gradle
```bash
./gradlew assembleRelease
```

3. Install the APK
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

## Usage

### Adding Accounts

#### QR Code
1. Tap the QR code button (bottom right)
2. Grant camera permission
3. Scan the QR code from your service provider
4. Account is automatically added

#### Manual Entry
1. Tap the + button (bottom right)
2. Enter:
   - Issuer (optional, e.g., "Google")
   - Account name (e.g., "user@example.com")
   - Secret key (Base32 encoded)
3. Optionally configure advanced settings:
   - Algorithm (SHA-1, SHA-256, SHA-512)
   - Digits (6 or 8)
   - Period (seconds)

### Using Codes
- Tap any account to copy the code to clipboard
- The circular timer shows remaining validity
- Codes auto-refresh every period (usually 30s)
- Clipboard is auto-cleared after 30 seconds

### Security Settings
1. Tap the settings icon (top right)
2. Set up PIN (4+ digits)
3. Enable biometric unlock (if available)
4. App will require authentication after 30 seconds in background

### Backup and Restore
Backups are encrypted with a password you provide:

1. Settings → Backup
2. Choose export location
3. Enter a strong password
4. Store backup file securely

To restore:
1. Settings → Restore
2. Select backup file
3. Enter the password

## Security Architecture

### Encryption Layers
1. **Android Keystore** - Generates and stores AES-256 master key
2. **AES-GCM Encryption** - All TOTP secrets encrypted before storage
3. **EncryptedSharedPreferences** - Additional encryption layer for storage
4. **PBKDF2** - Password-based encryption for backups (100,000 iterations)

### Security Measures
- No network access (internet permission explicitly removed)
- Screenshot protection via FLAG_SECURE
- Clipboard auto-clear
- Secrets never stored in plaintext
- Hardware-backed encryption when available
- Auto-lock on background

## Project Structure

```
app/src/main/java/com/makimakey/
├── crypto/
│   ├── Base32Decoder.kt       # RFC 4648 Base32 decoder
│   ├── TotpGenerator.kt       # RFC 6238 TOTP generator
│   └── EncryptionManager.kt   # Android Keystore encryption
├── storage/
│   └── SecureStorage.kt       # EncryptedSharedPreferences wrapper
├── qr/
│   └── OtpAuthParser.kt       # otpauth:// URI parser
├── security/
│   └── AppLockManager.kt      # PIN and biometric authentication
├── domain/
│   ├── model/
│   │   └── TotpAccount.kt     # Account data model
│   └── repository/
│       └── TotpRepository.kt  # Account management
├── ui/
│   ├── screens/               # Jetpack Compose screens
│   ├── components/            # Reusable UI components
│   └── theme/                 # Material 3 theme (true black)
└── util/
    └── BackupManager.kt       # Encrypted backup/restore
```

## Why MakimaKey?

### Privacy
- **No accounts** - No sign-up required
- **No analytics** - Zero telemetry or tracking
- **No ads** - Completely free and ad-free
- **No cloud** - All data stays on your device
- **Open source** - Auditable code

### Offline First
MakimaKey is designed to work completely offline. It will never request internet access, and all data remains local to your device. This means:
- Your TOTP secrets never leave your device
- No risk of cloud breaches
- Works on devices without network access
- Complete control over your data

### Compatibility
Compatible with any service that uses standard TOTP (RFC 6238), including:
- Google
- GitHub
- Microsoft
- Amazon
- Dropbox
- Facebook
- Twitter
- And thousands more...

## Important Disclaimers

### Data Loss Warning
- If you lose your device, you lose your accounts
- If you forget your PIN, you lose your accounts
- Always keep backup codes from service providers
- Export encrypted backups regularly
- Store backup files securely (not on the same device)

### Not a Password Manager
MakimaKey only generates TOTP codes. It does not:
- Store passwords
- Sync across devices
- Provide cloud backup
- Recover lost accounts

### Backup Codes
Always save the backup codes provided by service providers when setting up 2FA. These are your recovery method if you lose access to MakimaKey.

## Technical Details

### TOTP Implementation
- RFC 6238 compliant
- Proper HMAC-based dynamic truncation
- Support for SHA-1, SHA-256, SHA-512
- Configurable time step (default 30s)
- 6 or 8 digit codes
- Time counter based on Unix epoch

### Encryption
- AES-256-GCM for secret encryption
- Hardware-backed Android Keystore when available
- EncryptedSharedPreferences for storage
- PBKDF2 with 100,000 iterations for backups

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Follow Kotlin coding conventions
4. Add tests for new functionality
5. Submit a pull request

## License

[MIT](LICENSE) - see LICENSE file

## Acknowledgments

- Built with Jetpack Compose
- Uses AndroidX Security library
- ZXing for QR code parsing
- CameraX for camera integration

## Support

This is an open-source project maintained by volunteers. For issues and feature requests, please use GitHub Issues.

## Disclaimer

This software is provided "as is" without warranty. Use at your own risk. Always keep backup codes from your service providers.

## Author

**R** (rishabnotfound)

---

Made with ❤️ by R

