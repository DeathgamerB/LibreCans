# GitHub Actions Secrets Guide for Android Releases

This guide provides step-by-step instructions to generate your Android Keystore securely, extract the Base64 representation needed by GitHub Actions, and back it up in your Vaultwarden instance.

## 1. Generate the Android Keystore
You need a keystore file (.jks) to sign the APK. This ensures only you can release updates to the app.
Run the following command in your terminal. When prompted, fill out the required details. You'll need to remember the **Keystore Password**, **Key Alias**, and **Key Password**.

```bash
keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

## 2. Encode the Keystore as Base64
GitHub Actions cannot store binary files directly, so you must encode the keystore file as a Base64 string.
Run this command in the directory containing `release-key.jks`:

**On macOS/Linux:**
```bash
base64 -i release-key.jks | tr -d '\n' | xclip -selection clipboard
# (Use pbcopy instead of xclip on macOS)
```

**On Windows (PowerShell):**
```powershell
[convert]::ToBase64String((Get-Content -path "release-key.jks" -Encoding byte)) | Set-Clipboard
```

## 3. Save to Vaultwarden
Since this is a sensitive cryptographic key that allows anyone who holds it to impersonate your app updates, you must store it securely.
1. Open your Vaultwarden instance.
2. Click **Add Item** -> **Secure Note**.
3. Set the name to "LibreCans App Keystore".
4. Add the following details to the Note block:
   - **Key Alias:** (e.g., `my-key-alias`)
   - **Keystore Password:** (Your chosen password)
   - **Key Password:** (Your chosen password)
5. Under custom fields or directly in the note, paste the **Base64 string** generated in Step 2.
6. Also, use the File Attachment feature in Vaultwarden (if enabled) to upload the raw binary `release-key.jks` file itself so you never lose the original source.
7. Click **Save**.

## 4. Add Secrets to GitHub
Go to your GitHub Repository -> **Settings** -> **Secrets and variables** -> **Actions**.
Click **New repository secret** for each of the following:

- `RELEASE_SIGNING_KEY`: Paste the Base64 string.
- `RELEASE_KEY_ALIAS`: Paste the alias (e.g., `my-key-alias`).
- `RELEASE_STORE_PASSWORD`: Paste your keystore password.
- `RELEASE_KEY_PASSWORD`: Paste your key password.

Once configured, the CI/CD pipeline will automatically build and sign your apps for Debug and Release using this shared certificate.
