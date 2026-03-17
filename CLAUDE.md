# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**EncryptAction** is a Kotlin/Compose Android app demonstrating PKI-based secure file sharing (assessment brief: hybrid encryption + ECDSA signing for a small consultancy). minSdk 31, targetSdk 36.

## Build & Test Commands

```bash
./gradlew build                   # Full build
./gradlew assembleDebug           # Debug APK
./gradlew test                    # Unit tests (JVM)
./gradlew connectedAndroidTest    # Instrumented tests (requires device/emulator)
./gradlew clean                   # Clean build outputs
./gradlew lint                    # Static analysis
```

Run a single test class:
```bash
./gradlew test --tests "com.example.encryptaction.SomeTest"
```

## Architecture

Clean Architecture with three layers:

```
ui/          → Screens + ViewModels (Compose, Hilt @HiltViewModel, StateFlow)
domain/      → Models, repository interfaces, use cases (pure Kotlin, no Android deps)
data/        → Repository impls, Room DB (entities/DAOs/mappers), DataStore, crypto engines
di/          → Hilt modules wiring data layer to domain interfaces
core/crypto/ → Crypto engines and KeyStore manager (no business logic)
```

**Key principle:** domain layer has zero Android/data dependencies. Use cases depend only on domain repository interfaces. Hilt modules in `di/` wire the implementations.

## Cryptography

The `.eaep` file format encapsulates a hybrid encrypt+sign scheme:

| Field | Algorithm | Purpose |
|---|---|---|
| Wrapped AES key | RSA-4096-OAEP | Encrypts per-file AES key with recipient's public key |
| Ciphertext | AES-256-GCM (12-byte IV, 128-bit tag) | Encrypts file content |
| Signature | ECDSA P-256 / SHA-256 | Signs the encrypted payload (non-repudiation) |
| Sender key ID | String | Identifies which key bundle to verify against |

Binary layout: `[magic 0x45414550][version 0x01][wrappedKeyLen][wrappedKey][sigLen][sig][senderKeyIdLen][senderKeyId][IV+ciphertext]`

- `HybridCryptoEngine.kt` — top-level encrypt+sign / verify+decrypt
- `KeyStoreManager.kt` — RSA & EC key pairs via Android Keystore (hardware-backed)
- `AesCryptoEngine.kt`, `RsaCryptoEngine.kt`, `SigningEngine.kt` — single-responsibility engines
- `CryptoResult<T>` — sealed Success/Failure used throughout; never throw from crypto code

## State & Navigation

- Each screen has a paired `@HiltViewModel`. UI state is a single data class in `MutableStateFlow`.
- `MainViewModel` observes `SessionRepository` to decide initial route (login vs. home).
- Navigation routes are a sealed class in `NavRoutes`. Bottom nav items carry a `requiredRole` for RBAC (ADMIN vs MEMBER).
- Session persisted in DataStore; crypto keys stored in Android Keystore by alias tied to userId.

## Data Layer Conventions

- Room entities live in `data/local/` with mappers that convert to/from domain models. Never pass entities to domain or UI layers.
- Repository implementations in `data/repository/` implement domain interfaces; bound in `RepositoryModule`.
- `UserKeyBundle` stores DER-encoded public keys + fingerprint; PEM export/import handled by use cases.

## Feature Priority (per brief)

1. Key generation / PEM export / import
2. File encryption (hybrid)
3. File decryption + signature verification
4. Detached signing / verification
5. User accounts (local, admin + member roles)

No real backend — all data is local. Encrypted messaging is a stretch goal not in the brief.
