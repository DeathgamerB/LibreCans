# Project Information

## Local Development Setup

To build and run LibreCans locally, ensure you meet the following requirements:

- **Java Development Kit (JDK):** Version 21 is required.
- **Android SDK:** Ensure your SDK path is configured. If it's not automatically detected by Android Studio, create a `local.properties` file in the root directory with the path to your SDK:
  ```properties
  sdk.dir=/path/to/android/sdk
  ```

### Building the App

To compile a debug APK that can be installed on an Android device or emulator:
```bash
./gradlew :app:assembleDebug
```
The compiled APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Running Tests

Before submitting a pull request, verify that no regressions have been introduced by running the protocol and unit tests:
```bash
./gradlew :protocol:test
```

To compile and verify the UI test suite:
```bash
./gradlew :app:assembleAndroidTest
```

## Protocol References

LibreCans is a permitted derivative of other excellent open-source projects. If you are interested in helping expand the device compatibility or investigating new capabilities, you can find the upstream reference implementations and protocol research here:

- **SonyBridge**: [AmitRajput-Dev/SonyBridge](https://github.com/AmitRajput-Dev/SonyBridge)
- **SonyHeadphonesClient**: [Plutoberth/SonyHeadphonesClient](https://github.com/Plutoberth/SonyHeadphonesClient)
- **GadgetBridge**: Protocol implementations for various Sony devices.

If you add a new capability or device support, please ensure it gracefully falls back (or is capability-gated) for existing devices that might not support the new feature.
