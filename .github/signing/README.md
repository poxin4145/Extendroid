# CI signing key

`extendroid-release.p12` (PKCS#12, key alias `extendroid`) is the key GitHub Actions uses to sign
release APKs of this fork. Its password sits next to it in [`password.txt`](password.txt).

Committing a signing key is generally a bad idea. Here it only establishes the identity of builds
produced by this repository, and anyone able to push to the repository can already build arbitrary
APKs. Keeping it in the tree means every release can be installed over the previous one — Android
rejects an update signed with a different key.

Want your own key instead? Either replace both files, or add these repository secrets
(**Settings → Secrets and variables → Actions**), which take precedence:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | `base64 -w0 your-release.jks` |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias (defaults to `extendroid`) |
| `KEY_PASSWORD` | key password (defaults to the keystore password) |
