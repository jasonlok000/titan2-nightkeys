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
  const demo = phone.closest('.demo');
  const autoButton = document.querySelector('#demo-auto');
  const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
  const roomWord = lux => lux < 5 ? 'Dark' : lux < 10 ? 'Dim' : lux < 15 ? 'In between' : 'Bright';
  // Same as the app's defaults: on below 10 lux, off at 15 lux, so it never flickers.
  let lit = true;
  let offTimer;
  function render() {
    const lux = Number(slider.value);
    clearTimeout(offTimer);
    const desired = lit ? lux < 15 : lux < 10;
    if (lit && !desired) {
      offTimer = setTimeout(() => { lit = false; paint(); }, 250);
    } else {
      lit = desired;
    }
    paint();
  }
  function paint() {
    const lux = Number(slider.value);
    const day = Math.min(1, Math.max(0, (lux - 3) / 17));
    demo.style.setProperty('--day', day.toFixed(2));
    demo.dataset.room = day >= .5 ? 'bright' : 'dark';
    phone.dataset.lit = String(lit);
    output.value = roomWord(lux);
    slider.setAttribute('aria-valuetext', roomWord(lux));
    const room = lux < 10 ? 'Dark room' : lux < 15 ? 'Getting brighter' : 'Bright room';
    status.textContent = `${room} · keyboard light ${lit ? 'on' : 'off'}`;
    screenTitle.textContent = lit ? 'NightKeys' : 'Daylight';
    screenCaption.textContent = lit ? 'A little light. Just enough.' : 'No extra light needed.';
    for (const button of presets) {
      const selected = button.dataset.preset === 'dark' ? lux < 10 : lux >= 15;
      button.setAttribute('aria-pressed', String(selected));
    }
  }
  // Gentle tour between the two rooms until the visitor takes over.
  let autoTimer;
  let autoOn = !reducedMotion.matches;
  let visible = true;
  function schedule() {
    clearTimeout(autoTimer);
    autoButton.textContent = autoOn ? 'Pause auto-switch' : 'Play auto-switch';
    if (!autoOn || !visible || document.hidden) return;
    autoTimer = setTimeout(() => {
      slider.value = Number(slider.value) < 15 ? '30' : '3';
      render(); schedule();
    }, 3500);
  }
  function stopAuto() { autoOn = false; schedule(); }
  slider.addEventListener('input', () => { stopAuto(); render(); });
  presets.forEach(button => button.addEventListener('click', () => {
    stopAuto();
    slider.value = button.dataset.preset === 'dark' ? '3' : '30';
    render();
  }));
  autoButton.addEventListener('click', () => { autoOn = !autoOn; schedule(); });
  document.addEventListener('visibilitychange', schedule);
  if ('IntersectionObserver' in window) new IntersectionObserver(entries => {
    visible = entries[0].isIntersecting; schedule();
  }).observe(demo);
  render(); schedule();
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
