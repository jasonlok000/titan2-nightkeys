# Contributing

Thanks for making a small, useful phone tool better. Documentation and firmware reports are as valuable as code.

## No-code contributions

1. Read the setup guide and existing issues before opening a duplicate.
2. Select **New issue** and choose Bug, Firmware compatibility, or Feature / new tool.
3. State the app version, exact Elite firmware and steps you took. Describe physical LED behavior separately from a setting readback.
4. For diagnostics, choose **Copy support report** inside NightKeys. Review the text before posting publicly. Never share pairing codes, passwords, signing keys, private notifications or account details.
5. To improve a page, open `site/setup.html` or a Markdown doc, click GitHub's pencil, edit, and propose changes. GitHub can create a fork for you.

## Code contributions

```sh
git clone https://github.com/YOUR-ACCOUNT/titan2-nightkeys.git
cd titan2-nightkeys
git remote add upstream https://github.com/jasonlok000/titan2-nightkeys.git
git switch -c fix/short-description
# Make one focused change.
bash tools/test-core.sh
python3 tools/check-source.py
bash gradlew :app:lintDebug :app:assembleDebug
# For website edits, also follow docs/WEBSITE.md.
git add <changed-files>
git commit -m "Explain the behavior changed"
git push -u origin fix/short-description
```

Open a pull request from your branch to upstream `main`. Explain the problem, approach, tests actually run, and any missing device coverage. Include desktop/mobile screenshots for website changes. A successful emulator run does not verify Unihertz firmware.

## Safety review rules

Do not broaden the privileged command allow-list, accept arbitrary shell commands, hardcode a guessed hardware brightness, add input monitoring, or silently discard a recovery journal. New permissions need an explanation and privacy review. Changes to screen-off behavior, timeout values, concurrent hardware writes, restart or restore need fault tests and a real-device report.

The beta intentionally preserves the working 0.9.7 algorithm. A bounded renewable timeout is a future experiment, not an approved substitute. See `docs/ARCHITECTURE.md`.

## Add a future tool

Open a proposal first: problem, supported firmware, required permissions, cleanup/recovery, source repository and user guide. Add a reviewed entry to `site/data/tools.json`; only `status: available` tools are rendered. Keep NightKeys separate unless shared code has a clear contract and tests. No fake coming-soon products.

Use Java 17 and readable native Android components. Keep user-facing app strings in resources, web controls accessible, guides usable without JavaScript, and changes small. Contributions are accepted under the project's MIT license; retain all third-party notices. Do not submit code you cannot license.

The maintainer reviews changes before release. Never modify release signing assets or publishing workflows without explicit review. Follow [the conduct policy](CODE_OF_CONDUCT.md).
