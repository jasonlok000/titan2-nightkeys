# Offline signing and public release assembly

Normal releases use `release.yml` and the permanent signing key in repository
secrets, as described in [RELEASING.md](RELEASING.md). When those secrets have not
been configured, an owner can instead sign the exact CI-produced unsigned release
APK privately with the same permanent key and official Android `apksigner`.

Beta.2 follows that path. The private keystore and passwords stay outside GitHub.
`.release/public-signatures/1.0.0-beta.2.json` contains only public signatures,
certificate fingerprint, exact input/output hashes, and the source/CI artifact
identifiers. Its compressed data is an APK Signing Block produced by Android SDK
Build-Tools 35.0.0, not executable code or an encrypted private key.

`tools/apply-offline-signature.py` reconstructs only that exact signed file. It
checks both hashes, limits the block size, verifies the pinned public fingerprint,
and refuses other inputs. It does not create signatures or confer signing access.
The final workflow independently verifies the resulting APK using the official
`apksigner`, `zipalign`, and package metadata before publishing. The release source
archive is the exact source SHA whose CI produced the APK, not a later workflow
commit. BUILD-INFO records both the build run and the publication run.

The publication workflow also installs beta.1 and upgrades it in place to beta.2
on an Android 16 emulator, then launches the release app. That tests update-key
continuity, not Unihertz LEDs. Re-running a request refuses to overwrite an
existing release. A new version needs a new successful CI build and new private
signing; these public signatures cannot authorize changed application contents.

Beta.2 was the last offline-signed release. The original key was lost; from beta.3
releases are signed in CI with a replacement key (see RELEASING.md). These beta.2
public signatures remain valid only for the beta.2 artifact.

Never commit the `.p12`, its passwords, or the maintainer backup. Keep redundant
private backups of the permanent key. Do not generate a replacement release key.
