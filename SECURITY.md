# Security policy

NightKeys executes a narrow set of privileged operations after Shizuku authorization. Treat changes to that path, APK signing and recovery with extra care.

For a suspected vulnerability, use GitHub's **Security → Report a vulnerability** if private reporting is enabled. Otherwise open an issue saying only that you need a private security contact; do not post exploit details, secrets, personal device data or an attack against another user. The maintainer will arrange an appropriate reporting channel. Do not post signing keys, tokens, pairing codes or passwords.

Community beta releases are the supported branch. Historical 0.9.x trial APKs and temporary build branches are not maintained distribution channels. No response-time SLA is promised.

Known limitations are not hidden: see docs/SAFETY.md. No independent watchdog guarantees restoration after simultaneous app/helper failure. Source review and permission prompts do not guarantee compatibility with untested firmware. Download release APKs only from this repository and verify the published signing certificate and checksum when necessary.
