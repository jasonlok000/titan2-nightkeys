# Public signing metadata only

`backup-recipient.pem` is a PUBLIC encryption-recipient certificate used for the one-time encrypted signing-key backup. It cannot sign Android APKs or decrypt the backup.

`certificate.sha256` is the PUBLIC fingerprint of the permanent Android release signing certificate, recorded during the first release. Future releases must match it.

Never commit the recipient private key, Android keystore, passwords, tokens, or plaintext backup. Encrypted CI backup artifacts still expire; the maintainer must save the private backup securely. See docs/RELEASING.md.
