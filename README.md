# CaptureSposed
<img align="left" src="images/ic_launcher-playstore.png" width="115" />

With the release of Android 14, Google added an API to enable app developers to detect screenshots. This API has since been adopted by popular apps such as Snapchat.

CaptureSposed is an Xposed module that effectively disables this API as well as the screen recording detection API added in Android 15.

Hooks run entirely client-side inside each scoped app's own process, so the module works whether it is loaded system-wide via **LSPosed** (requires root) or embedded directly into a single target app via **LSPatch** (no root required).

> This is a fork of the original [CaptureSposed](https://github.com/99keshav99/CaptureSposed) project.

**⚠️ WARNING:** CaptureSposed targets Android 14 or newer. This module cannot be guaranteed to work on all devices and apps. Use at your own risk. Additionally, this module does not protect against screenshot detection from apps that use the pre-Android 14 approach of using file system listeners to detect screenshots ([ref 1](https://abangfadli.medium.com/shotwatch-android-screenshot-detector-library-6a75d7242109), [ref 2](https://viveksb007.wordpress.com/2017/11/10/how-snapchat-detects-when-screenshot-is-taken-hypothesis/)).

### Option A: LSPosed (requires root)
1. Install LSPosed. This requires your device to be rooted.
2. Install CaptureSposed.
3. Add the target app(s) to CaptureSposed's scope in the LSPosed Manager and activate the module.
4. Reboot your device and sign in.
5. Open the target app and attempt to take a screenshot / screen recording. If it is not detected, the module is working as intended.

### Option B: LSPatch (no root required)
1. Install [LSPatch](https://github.com/LSPosed/LSPatch).
2. Patch the target app's APK, embedding CaptureSposed as a module.
3. Install the patched APK.
4. Open the patched app and attempt to take a screenshot / screen recording. If it is not detected, the module is working as intended.

## Building

This project uses Gradle with a Kotlin DSL and depends on the [`libxposed/api`](https://github.com/libxposed/api) and [`libxposed/service`](https://github.com/libxposed/service) git submodules.

```sh
git clone --recurse-submodules <this-repo-url>
cd CaptureSposed
./gradlew assembleDebug
```

If you already cloned without `--recurse-submodules`, run:

```sh
git submodule update --init --recursive
```

The debug APK is written to `app/build/outputs/apk/debug/`. A GitHub Actions workflow ([`.github/workflows/build-debug.yml`](.github/workflows/build-debug.yml)) also builds the debug APK on every push/PR and uploads it as an artifact.

## License

See [LICENSE](LICENSE).
