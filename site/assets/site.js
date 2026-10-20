'use strict';
document.documentElement.classList.add('js');
// Illustration only: no phone connection and no access to a real sensor.
const phone = document.querySelector('[data-phone]');
if (phone) {
  const slider = document.querySelector('#room-light');
  const output = document.querySelector('#light-output');
  const status = document.querySelector('#demo-status');
  const screenTitle = document.querySelector('.screen-title');
  const screenCaption = document.querySelector('.screen-caption');
  const presets = [...document.querySelectorAll('[data-preset]')];
  let screenOn = true;
  let lit = true;
  let offTimer;
  function render() {
    const lux = Number(slider.value);
    clearTimeout(offTimer);
    const desired = screenOn && (lit ? lux < 15 : lux < 10);
    if (lit && !desired && screenOn) {
      offTimer = setTimeout(() => { lit = false; paint(); }, 250);
    } else {
      lit = desired;
    }
    paint();
  }
  function paint() {
    const lux = Number(slider.value);
    phone.dataset.lit = String(lit && screenOn);
    phone.dataset.screen = String(screenOn);
    output.value = `${lux} lux`;
    slider.setAttribute('aria-valuetext', `${lux} lux`);
    status.textContent = !screenOn ? 'Screen off · keyboard off' : lit ? 'Dark room · keyboard on' : 'Bright enough · keyboard off';
    screenTitle.textContent = lit ? 'NightKeys' : 'Daylight';
    screenCaption.textContent = lit ? 'A little light. Just enough.' : 'No extra light needed.';
    for (const button of presets) {
      const selected = button.dataset.preset === 'sleep' ? !screenOn
        : button.dataset.preset === 'dark' ? screenOn && lux < 10 : screenOn && lux >= 15;
      button.setAttribute('aria-pressed', String(selected));
    }
  }
  slider.addEventListener('input', () => { screenOn = true; render(); });
  presets.forEach(button => button.addEventListener('click', () => {
    screenOn = button.dataset.preset !== 'sleep';
    if (button.dataset.preset === 'dark') slider.value = '3';
    if (button.dataset.preset === 'bright') slider.value = '30';
    if (!screenOn) lit = false;
    render();
  }));
  render();
}
// Optional setup progress is stored only on this browser, never submitted.
const checks = [...document.querySelectorAll('[data-step-check]')];
if (checks.length) {
  const key = 'titan-tools-setup-v2';
  let saved = {};
  try { saved = JSON.parse(localStorage.getItem(key) || '{}') || {}; } catch (_) { }
  const update = () => {
    for (const box of checks) box.closest('.step').dataset.done = String(box.checked);
    const progress = document.querySelector('#setup-progress');
    if (progress) progress.textContent = `${checks.filter(c => c.checked).length} of ${checks.length} steps complete`;
  };
  checks.forEach(box => {
    box.checked = saved[box.dataset.stepCheck] === true;
    box.addEventListener('change', () => {
      saved[box.dataset.stepCheck] = box.checked;
      try { localStorage.setItem(key, JSON.stringify(saved)); } catch (_) { }
      update();
    });
  });
  document.querySelector('#reset-progress').addEventListener('click', () => {
    saved = {}; checks.forEach(box => { box.checked = false; });
    try { localStorage.removeItem(key); } catch (_) { }
    update();
  });
  update();
}
// Static NightKeys markup remains complete when JavaScript or fetching is unavailable.
const catalogue = document.querySelector('#tool-catalogue');
if (catalogue) fetch('data/tools.json').then(response => {
  if (!response.ok) throw new Error('Catalogue unavailable');
  return response.json();
}).then(data => {
  if (!Array.isArray(data.tools)) return;
  for (const tool of data.tools.filter(t => t.id !== 'nightkeys' && t.status === 'available')) {
    if (!/^[a-z0-9-]+$/.test(tool.id)) continue;
    const source = new URL(tool.source);
    if (source.protocol !== 'https:' || source.hostname !== 'github.com') continue;
    const article = document.createElement('article'); article.className = 'tool-row';
    const icon = document.createElement('div'); icon.className = 'tool-icon'; icon.textContent = '↗'; icon.setAttribute('aria-hidden', 'true');
    const copy = document.createElement('div'); copy.className = 'tool-copy';
    const heading = document.createElement('h3'); heading.textContent = tool.name;
    const description = document.createElement('p'); description.textContent = tool.description;
    copy.append(heading, description);
    const link = document.createElement('a'); link.className = 'text-link'; link.href = source.href; link.textContent = 'Source & setup';
    article.append(icon, copy, link); catalogue.append(article);
  }
}).catch(() => { /* Keep the static catalogue. */ });
