# 🛰️ Aetherion Mobile NOC

**Developer:** Mohammad Abdalftah Ibrahime  
**Architecture:** MVVM + Clean Architecture  
**Platform:** Android (minSdk 26 / targetSdk 35)

---

## 📁 هيكل المشروع

```
AetherionNOC/
├── .github/
│   └── workflows/
│       └── android-build.yml        ← GitHub Actions CI/CD
├── app/
│   ├── src/
│   │   ├── main/kotlin/com/aetherion/noc/
│   │   │   ├── core/            ← DI, Logging, Network, Security
│   │   │   ├── data/            ← API, DTOs, Room, Repositories
│   │   │   ├── domain/          ← Models, UseCases, Interfaces
│   │   │   └── presentation/    ← Compose UI, ViewModels
│   │   ├── test/                ← Unit Tests
│   │   └── androidTest/         ← Integration Tests
│   ├── build.gradle.kts
│   ├── google-services.json.placeholder
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml       ← Version Catalog
├── keystore/
│   └── README.md                ← تعليمات الـ Keystore
├── build.gradle.kts
├── settings.gradle.kts
├── .gitignore
└── README.md
```

---

## 🚀 إعداد GitHub Secrets

اذهب إلى `Settings > Secrets and variables > Actions` وأضف:

| Secret | الوصف |
|--------|-------|
| `GOOGLE_SERVICES_JSON` | محتوى `google-services.json` مشفراً بـ Base64 |
| `KEYSTORE_BASE64` | ملف `.jks` مشفراً بـ Base64 |
| `KEYSTORE_PASSWORD` | كلمة مرور الـ Keystore |
| `KEY_ALIAS` | اسم الـ Key (مثال: `aetherion`) |
| `KEY_PASSWORD` | كلمة مرور الـ Key |

```bash
# لتشفير الملفات إلى Base64
base64 -w 0 app/google-services.json
base64 -w 0 keystore/aetherion-release.jks
```

---

## 🔄 Workflow Summary

| الحدث | الإجراء |
|-------|---------|
| Push → `main` | Lint + Tests + Debug APK + **Release AAB** |
| Push → `develop` | Lint + Tests + Debug APK |
| Pull Request → `main` | Lint + Tests |
| Manual Dispatch | اختيار Flavor + Build Type |

---

## 🛠️ البناء محلياً

```bash
./gradlew assembleDevDebug          # Debug APK
./gradlew bundleProdRelease         # Production AAB (يحتاج keystore env vars)
./gradlew testDevDebugUnitTest      # Unit Tests
./gradlew lintDevDebug              # Lint
```

---

## 🔐 الأمان

- Certificate Pinning (Production)
- Biometric Authentication
- EncryptedSharedPreferences
- ProGuard/R8 في Release builds
