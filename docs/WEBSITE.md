# Titan Tools website

`site/` is a dependency-free static site. There is no framework build step. Edit the HTML, CSS, JavaScript and JSON directly; do not edit screenshots as UI. Original system-font design: dark ink, pale paper, sage-green light, generous type and an interactive illustrated physical keyboard. The illustration is not a phone photograph or a real connection.

```sh
python3 -m http.server 8765 --directory site
# Open http://localhost:8765/
python3 tools/check-site.py
# Optional browser suite (CI installs the pinned test dependencies):
python3 tools/test-site.py
```

`index.html`: short showcase and catalogue. `setup.html`: a download start card, exactly three `.step` sections (step 2 is split into parts A–D: Developer options, Wireless debugging, pairing, start & connect), each ending in a “You should now see” checkpoint, then a “Stuck?” help section. The restart reminder sits at the top. Optional local checklist. `scale-toggle.html`: Scale Toggle showcase (the same Titan illustration switching between a one-column phone layout and a two-pane tablet layout when the red side key is long-pressed, with the app’s exact `Phone mode` / `Tablet mode` toasts) and its own three-step guide: start Shizuku (linking to NightKeys parts for install and pairing), allow, then bind to a long press of the side button. Its checklist uses its own browser storage key. The APK is published as the `scaletoggle-v<version>` release of this repository; that tag does not match the `v*` NightKeys release workflow and is never marked latest. `hold-symbols.html`: Hold Symbols showcase (the same Titan illustration with a message being typed; visitors tap or hold the drawn keys, a big “Press and hold S” button, or their own keyboard, and a held key pops up the symbol printed on it after the app’s default 400 ms; an auto demo types “see you at 4:30!” until the visitor takes over, never with reduced motion), a symbol map laid out like the keyboard, and its own three-step guide: start Shizuku, allow (Shizuku plus the accessibility service, including Android’s restricted-setting unlock), then turn on and try it. The APK is published as the `holdsymbols-v<version>` release of this repository, signed with the NightKeys key and never marked latest. The header lists the three tools and Contribute (Contribute moves to the footer below 420 px); the NightKeys guide is reached from its page. `contribute.html`: non-code, code and new-tool entry points. `privacy.html`: app/site privacy. CSS tokens are at the top of `assets/styles.css`. `assets/site.js` controls only illustration/checklist/catalogue. No third-party fonts, analytics or live phone APIs are used.

## Adding tools

Propose scope first. Add an object to `data/tools.json`: unique lowercase `id`, `name`, `description`, `status`, `releaseStage`, `source` HTTPS GitHub URL, `guide` and `platform`. Only reviewed `status: available` entries are rendered; NightKeys has a static no-JavaScript fallback. Add a full guide and real source/release before listing availability. A tool can have a separate repository and still share this home.

## QA

Check 1440 px desktop, 390 px mobile, 320 px small mobile and the Titan 2 Elite's 9:10 screen (360×400, 432×480 and a shorter 360×330 for browser bars). On short, wide screens (`max-width:760px` and `min-aspect-ratio:3/4`) the phone and its controls sit side by side and the whole demo must fit one screen. The Dark room / Bright room buttons must visibly change both the room and the keyboard light. Check keyboard navigation, focus, reduced motion, internal anchors, no horizontal scroll, no console errors, checklist persistence/reset, and the guide with JavaScript disabled. The catalogue uses textContent and validates source links, not arbitrary HTML.

## Hosting

GitHub Pages deploys `site/` through `.github/workflows/pages.yml`. An owner must enable **Settings → Pages → Build and deployment → Source: GitHub Actions** once. The normal workflow token cannot enable Pages administration by itself. Then use Actions → Deploy Titan Tools → Run workflow. Public URL: https://jasonlok000.github.io/titan2-nightkeys/ . A committed URL is not proof deployment succeeded; check the workflow and public page.

Keep setup button labels synchronized with Android string resources. Update version.properties, release JSON and guide download links together for each release. Do not publish a fake release URL or claim an untested firmware works.

## Titan 2 Elite illustration

The interactive handset is an original inline SVG, not a manufacturer photograph.
It follows the Elite's **117.8 × 75 mm** body proportions and **1080 × 1200** display,
with a top-left punch-hole, three full ten-key rows plus the shift/navigation/space/fn
row, and the red side shortcut button. The previous generic floating keycaps and
invented printed branding have been removed. The UI inside the screen is illustrative.
Reference: [Unihertz product page](https://www.unihertz.com/products/titan-2-elite),
front view `T2US-E1-US-01.jpg`. No manufacturer artwork is redistributed.
The `.key-legend`, `.key-alt`, `.key-icon` and `.elite-screen-content` classes expose
lighting states to the existing demo. The demo offers only Dark room and Bright room (the app still turns the light off with the screen; the demo just doesn't show it). `--day` on `.demo` cross-fades the night and day backgrounds and `data-room` reports which one is showing. It auto-switches every few seconds until the visitor interacts or pauses it, and never auto-switches with reduced motion. Without JavaScript it shows the dark room with no controls.
Setup has exactly three `.step` sections; download and optional help are not steps.
