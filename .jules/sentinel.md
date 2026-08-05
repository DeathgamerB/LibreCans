## 2024-05-18 - [Security Enhancement: Replace deprecated MasterKeys with MasterKey.Builder]
**Vulnerability:** The app used the deprecated `MasterKeys` class from `androidx.security:security-crypto` which relies on outdated initialization patterns.
**Learning:** To support newer, more secure encryption configurations and ensure long-term compatibility, it's better to update the dependency and use `MasterKey.Builder` with the appropriate `KeyScheme`.
**Prevention:** Regularly audit security dependencies and update to non-deprecated alternatives to leverage the latest security improvements and maintain standard practices.
