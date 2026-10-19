# Maintainer release procedure

## Identity

Application ID: `com.everyonesniche.nightkeys`. Debug adds `.debug`. Community version comes from `version.properties`; versionCode must always increase. The first release is 1.0.0-beta.1 / 1000001. Once the permanent community key is established, never replace it with a new debug or CI-generated key for an update.

## First release key bootstrap

The one-time first-release workflow generates a random permanent signing keystore on the hosted runner, creates an encrypted backup for the maintainer's public recipient certificate, and uploads only the **encrypted** backup as an Actions artifact. The recipient private key is never committed. APK signing then uses that keystore. The public APK, checksum, public certificate and source are published; secrets are not release assets.

The maintainer must securely retain the decrypted backup (keystore plus signing.json). It is required for future updates. Keep redundant offline/private backups. Do not upload it to issues, the source repo or release assets. Encrypted CI artifacts expire; they are not a long-term backup. `tools/install-signing-secrets.py` configures future release secrets from that private directory using the maintainer's locally authenticated GitHub CLI.

```sh
# On the maintainer's computer, after securely extracting the private backup:
gh auth login
python3 tools/install-signing-secrets.py /absolute/path/to/private-backup
```

The script sends the keystore and passwords through stdin, not printed command-line arguments. Review it first. It sets NIGHTKEYS_KEYSTORE_BASE64, NIGHTKEYS_STORE_PASSWORD, NIGHTKEYS_KEY_ALIAS and NIGHTKEYS_KEY_PASSWORD for this repo. No one needs to paste credentials into a chat. After bootstrap, the public signing certificate is the identity pin; future publishing refuses a mismatch or missing secrets.

## Subsequent releases

1. Finish CI and real-device acceptance from TESTING.md. Do not treat emulator results as Titan hardware coverage.
2. Increment version.properties. Update CHANGELOG.md, `site/data/release.json`, and download links in README/setup pages. Keep beta labels until evidence warrants stable.
3. Ensure the repository signing secrets are installed and the certificate pin matches the previous public APK. Do not rotate it casually.
4. Open Actions → Publish release → Run workflow from main (or push a matching version tag). The workflow reruns checks, builds with the permanent key, verifies signature/alignment and certificate continuity, then creates release assets.
5. Review release notes: tested firmware, actual CI result, known persistent-setting risks, restart/removal steps and migration instructions.
6. Install the signed release on a phone after safe restoration. Also test an ordinary same-key upgrade from the previous community version.
7. Verify the public download and Pages links. Never replace an already released APK silently; issue a new version.

Local signing uses NIGHTKEYS_KEYSTORE (absolute path), NIGHTKEYS_STORE_PASSWORD, NIGHTKEYS_KEY_ALIAS and NIGHTKEYS_KEY_PASSWORD with `gradlew :app:assembleRelease`. An unsigned release is not an installable distribution.

## Owner settings

Enable GitHub Pages once as documented in WEBSITE.md. Configure branch protection/rulesets to require CI and review changes to signing/publishing paths. The connected content editor may not have administrative tool actions; do not assume these settings were configured just because workflow files exist.

Source is built normally by Gradle. Do not reintroduce patched-DEX APK assembly. Do not claim byte-for-byte reproducibility without independently reproducing the artifact.
