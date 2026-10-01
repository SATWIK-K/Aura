# Aura — Weight Tracker

A premium Android weight-tracking app with a glassmorphism UI, daily logging,
goal progress, and 7 transparent blur widgets (One UI).

## Release 1.0.7 (stable)

Signed release APK: see **Releases** (Aura-1.0.7.apk).

## Build

Requires JDK 21 and the Android SDK (`ANDROID_HOME`).

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
./gradlew :app:testDebugUnitTest   # unit + Robolectric tests
./gradlew :app:assembleRelease     # signed APK (needs keystore.properties)
```

Release signing needs `keystore.properties` and the keystore file next to it
(both are git-ignored and intentionally not in this repo):

```properties
storeFile=aura-release.jks
storePassword=...
keyAlias=aura
keyPassword=...
```

## Widgets

Today, Trend, Goal ring, Streak, BMI, Compact, Progress — transparent shells
that pick up the One UI wallpaper blur, with adaptive native text and
per-size artwork.
