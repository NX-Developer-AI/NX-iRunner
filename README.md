# NX iRunner

> 🇹🇷 [Türkçe](#türkçe) · 🇬🇧 [English](#english)

---

## Türkçe

**NX iRunner**, iOS `.ipa` uygulamalarını Android'de çalıştıran bir başlatıcı ve emülatör paketidir.
Emülasyon çekirdeği olarak [touchHLE](https://github.com/touchHLE/touchHLE) (Rust, yüksek seviye emülatör)
kullanılır. NX iRunner bunun üstüne Kotlin + Jetpack Compose ile yazılmış bir kütüphane arayüzü ekler.

### Ne çalışır, ne çalışmaz

| Durum | Açıklama |
|---|---|
| ✅ | iPhone OS 2.x / 3.x, 32-bit ARM (armv6 / armv7) uygulamaları; özellikle oyunlar |
| 🟡 | iOS 4.0 tam, sonraki 4.x sürümleri kısmen destekleniyor |
| ❌ | iOS 5+ API'lerine dayanan uygulamalar, sadece arm64 içeren modern IPA'lar |
| ❌ | FairPlay ile şifreli (App Store'dan çekilmiş, şifresi çözülmemiş) IPA'lar |

Hangi uygulamaların çalıştığı için: <https://appdb.touchhle.org/>

### Özellikler

- SAF ile ya da "Birlikte aç / Paylaş" menüsünden `.ipa` içe aktarma
- IPA analizi: `Info.plist` (binary/XML), Mach-O fat/thin başlığı, mimari listesi, `LC_ENCRYPTION_INFO` ile şifre tespiti
- Apple CgBI PNG ikonlarını standart PNG'ye çeviren dönüştürücü
- Uyumluluk değerlendirmesi (Büyük ihtimalle çalışır / Kısmi / Düşük ihtimal / Uyumsuz)
- Uygulama başına başlatma seçenekleri: yön, cihaz ailesi, çözünürlük ölçeği, force composition, ağ erişimi, FPS sınırı, ek argümanlar
- Emülatör ayrı bir `:emulator` sürecinde çalışır, uygulama kapanınca süreç temizlenir
- Belge sağlayıcı: kayıtlara, uygulamalara ve loglara sistem dosya yöneticisinden erişim
- Emülatör log görüntüleyici (paylaşma, temizleme)
- Türkçe ve İngilizce arayüz; yeni dil eklemek için `res/values-xx/strings.xml` yeterli

### Kullanım

1. APK'yı kur (sadece **arm64-v8a** cihazlar).
2. **IPA içe aktar** butonuna bas ve şifresi çözülmüş bir `.ipa` seç.
3. Uygulamaya **dokun** → başlatır. **Uzun bas** → detaylar ve seçenekler.

### Proje yapısı

```
NX-iRunner/
├── android/                  Gradle projesi (Kotlin + Compose başlatıcı)
│   └── app/src/main/java/com/nxteam/irunner/
│       ├── ipa/              IPA, plist, Mach-O, CgBI analizi
│       ├── data/             Room, DataStore, kütüphane deposu
│       ├── emulator/         SDLActivity köprüsü, başlatma seçenekleri, belge sağlayıcı
│       └── ui/               Compose ekranları (MVVM)
├── core/touchHLE/            touchHLE submodule'ü (v0.3.0)
├── patches/touchHLE/         Çekirdeğe CI'da uygulanan yamalar
└── .github/workflows/        GitHub Actions build
```

### Çekirdek yamaları

| Yama | Amaç |
|---|---|
| `0001-android-launch-arguments.patch` | Android'de `SDL_main` argümanlarını okur; başlatıcı IPA yolunu ve seçenekleri doğrudan verir |
| `0002-android-provider-authority.patch` | Belge sağlayıcı adresini NX iRunner paketine yönlendirir |

### Build

Build GitHub Actions'ta yapılır (`.github/workflows/build.yml`): Rust + cargo-ndk + NDK r25c + Boost 1.81.
Her push'ta debug ve release APK artifact olarak yüklenir. `v*` etiketi atılırsa GitHub Release oluşturulur.

İsteğe bağlı release imzalama secret'ları: `NX_KEYSTORE_BASE64`, `NX_KEYSTORE_PASSWORD`, `NX_KEY_ALIAS`, `NX_KEY_PASSWORD`.
Tanımlı değilse release APK debug anahtarıyla imzalanır.

### Lisans

NX iRunner kaynak kodu **GPL-3.0-or-later** lisanslıdır. touchHLE kaynak kodu MPL-2.0, touchHLE binary'leri
GPL-3.0-or-later lisanslıdır; `touchHLE_dylibs/` ve `touchHLE_fonts/` kendi lisanslarına tabidir.
NX iRunner hiçbir Apple yazılımı ya da iOS uygulaması içermez.

---

## English

**NX iRunner** is a launcher and emulator package that runs iOS `.ipa` apps on Android.
It uses [touchHLE](https://github.com/touchHLE/touchHLE) (a Rust high-level emulator) as its emulation core
and adds a Kotlin + Jetpack Compose library front end on top of it.

### What works and what does not

| Status | Description |
|---|---|
| ✅ | iPhone OS 2.x / 3.x, 32-bit ARM (armv6 / armv7) apps, especially games |
| 🟡 | iOS 4.0 is supported; later 4.x releases are partial |
| ❌ | Apps that depend on iOS 5+ APIs, and modern IPAs that only contain arm64 code |
| ❌ | FairPlay-encrypted IPAs (pulled from the App Store without decryption) |

For which apps actually work, see <https://appdb.touchhle.org/>

### Features

- Import `.ipa` files through SAF or the "Open with / Share" menu
- IPA analysis: `Info.plist` (binary/XML), Mach-O fat/thin headers, architecture list, encryption detection via `LC_ENCRYPTION_INFO`
- Converter that turns Apple CgBI PNG icons into standard PNGs
- Compatibility rating (Likely works / Partial / Unlikely / Incompatible)
- Per-app launch options: orientation, device family, resolution scale, force composition, network access, FPS limit, extra arguments
- The emulator runs in a separate `:emulator` process, which is cleaned up when the app exits
- Documents provider: open save data, apps and logs in the system file manager
- Emulator log viewer (share, clear)
- Turkish and English UI. To add a language, add `res/values-xx/strings.xml`

### Usage

1. Install the APK (**arm64-v8a** devices only).
2. Tap **Import IPA** and pick a decrypted `.ipa`.
3. **Tap** an app to launch it. **Long-press** an app for details and options.

### Project layout

```
NX-iRunner/
├── android/                  Gradle project (Kotlin + Compose launcher)
│   └── app/src/main/java/com/nxteam/irunner/
│       ├── ipa/              IPA, plist, Mach-O and CgBI analysis
│       ├── data/             Room, DataStore, library repository
│       ├── emulator/         SDLActivity bridge, launch options, documents provider
│       └── ui/               Compose screens (MVVM)
├── core/touchHLE/            touchHLE submodule (v0.3.0)
├── patches/touchHLE/         Patches applied to the core in CI
└── .github/workflows/        GitHub Actions build
```

### Core patches

| Patch | Purpose |
|---|---|
| `0001-android-launch-arguments.patch` | Reads the `SDL_main` arguments on Android, so the launcher can pass the IPA path and options directly |
| `0002-android-provider-authority.patch` | Points the documents provider URI at the NX iRunner package |

### Build

Builds run on GitHub Actions (`.github/workflows/build.yml`) with Rust, cargo-ndk, NDK r25c and Boost 1.81.
Every push uploads the debug and release APKs as artifacts. Pushing a `v*` tag also creates a GitHub Release.

Optional release signing secrets: `NX_KEYSTORE_BASE64`, `NX_KEYSTORE_PASSWORD`, `NX_KEY_ALIAS`, `NX_KEY_PASSWORD`.
If they are not set, the release APK is signed with the debug key.

### License

The NX iRunner source code is licensed under **GPL-3.0-or-later**. The touchHLE source code is MPL-2.0, and
touchHLE binaries are GPL-3.0-or-later. `touchHLE_dylibs/` and `touchHLE_fonts/` have their own licenses.
NX iRunner does not include any Apple software or iOS apps.
