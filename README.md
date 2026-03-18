# EncryptAction

An Android app demonstrating PKI-based secure file sharing using hybrid encryption and digital signatures.

> Built as a university assessment project for a small consultancy use-case.

---

## Features

- **Key Management** — Generate RSA-4096 + ECDSA P-256 key pairs via Android Keystore; export/import public keys as PEM
- **File Encryption** — Hybrid AES-256-GCM + RSA-OAEP encryption producing `.eaep` files
- **File Decryption** — Decrypt `.eaep` files with signature verification
- **Detached Signing** — Sign and verify arbitrary files independently of encryption
- **User Accounts** — Local accounts with RBAC (Admin / Member roles)

---

## .eaep File Format

Custom binary format encapsulating the hybrid scheme:

| Field | Algorithm | Purpose |
|---|---|---|
| Magic + version | `0x45414550 0x01` | Format identifier |
| Wrapped AES key | RSA-4096-OAEP | Per-file AES key encrypted for recipient |
| Ciphertext | AES-256-GCM (12-byte IV, 128-bit tag) | Encrypted file content |
| Signature | ECDSA P-256 / SHA-256 | Signs the encrypted payload |
| Sender key ID | String | Identifies the signing key bundle |

---

## Architecture

Clean Architecture with three layers:

```
ui/          → Compose screens + Hilt ViewModels, StateFlow-based UI state
domain/      → Pure Kotlin models, repository interfaces, use cases
data/        → Room DB, DataStore, repository impls, crypto engines
core/crypto/ → HybridCryptoEngine, KeyStoreManager, AES/RSA/Signing engines
di/          → Hilt modules wiring data → domain interfaces
```

**Key rule:** the domain layer has zero Android dependencies. Crypto results use `CryptoResult<T>` (sealed Success/Failure) — crypto code never throws.

---

## Tech Stack

- Kotlin + Jetpack Compose
- Hilt (dependency injection)
- Room (local database)
- DataStore (session persistence)
- Android Keystore (hardware-backed key storage)
- minSdk 31 / targetSdk 36

---

## Build

```bash
./gradlew assembleDebug          # Debug APK
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests (device/emulator required)
./gradlew lint                   # Static analysis
```
