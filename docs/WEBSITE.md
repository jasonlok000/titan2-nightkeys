# Titan Tools website

`site/` is a dependency-free static site. There is no framework build step. Edit the HTML, CSS, JavaScript and JSON directly; do not edit screenshots as UI. Original system-font design: dark ink, pale paper, sage-green light, generous type and an interactive illustrated physical keyboard. The illustration is not a phone photograph or a real connection.

```sh
python3 -m http.server 8765 --directory site
# Open http://localhost:8765/
python3 tools/check-site.py
# Optional browser suite (CI installs the pinned test dependencies):
python3 tools/test-site.py
```

`index.html`: short showcase and catalogue. `setup.html`: eight detailed steps with optional local checklist. `contribute.html`: non-code, code and new-tool entry points. `privacy.html`: app/site privacy. CSS tokens are at the top of `assets/styles.css`. `assets/site.js` controls only illustration/checklist/catalogue. No third-party fonts, analytics or live phone APIs are used.

## Adding tools

Propose scope first. Add an object to `data/tools.json`: unique lowercase `id`, `name`, `description`, `status`, `releaseStage`, `source` HTTPS GitHub URL, `guide` and `platform`. Only reviewed `status: available` entries are rendered; NightKeys has a static no-JavaScript fallback. Add a full guide and real source/release before listing availability. A tool can have a separate repository and still share this home.

## QA

Check 1440 px desktop, 390 px mobile and 320 px small mobile. The light buttons and slider must visibly change the keyboard; screen off must darken the screen and keys. Check keyboard navigation, focus, reduced motion, internal anchors, no horizontal scroll, no console errors, checklist persistence/reset, and the guide with JavaScript disabled. The catalogue uses textContent and validates source links, not arbitrary HTML.

## Hosting

GitHub Pages deploys `site/` through `.github/workflows/pages.yml`. An owner must enable **Settings → Pages → Build and deployment → Source: GitHub Actions** once. The normal workflow token cannot enable Pages administration by itself. Then use Actions → Deploy Titan Tools → Run workflow. Public URL: https://jasonlok000.github.io/titan2-nightkeys/ . A committed URL is not proof deployment succeeded; check the workflow and public page.

Keep setup button labels synchronized with Android string resources. Update version.properties, release JSON and guide download links together for each release. Do not publish a fake release URL or claim an untested firmware works.
