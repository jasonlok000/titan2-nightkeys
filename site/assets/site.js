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
// Scale Toggle illustration: a long press on the side key flips the layout and shows the toast.
const scalePhone = document.querySelector('[data-scale-phone]');
if (scalePhone) {
  const demo = scalePhone.closest('.demo');
  const status = document.querySelector('#scale-status');
  const toastText = scalePhone.querySelector('.toast-text');
  const sideButton = document.querySelector('#side-press');
  const presets = [...document.querySelectorAll('[data-mode-preset]')];
  const autoButton = document.querySelector('#scale-auto');
  const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
  const words = { phone: 'Phone mode · big and easy to tap', tablet: 'Tablet mode · two panes side by side' };
  let mode = 'phone';
  let pressing = false;
  let toastTimer;
  function setMode(next) {
    if (next === mode) return;
    mode = next;
    scalePhone.dataset.mode = mode;
    demo.dataset.mode = mode;
    status.textContent = words[mode];
    for (const button of presets) button.setAttribute('aria-pressed', String(button.dataset.modePreset === mode));
    // Same text as the app's success toast.
    toastText.textContent = mode === 'tablet' ? 'Tablet mode' : 'Phone mode';
    scalePhone.dataset.toast = 'true';
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { scalePhone.dataset.toast = 'false'; }, 1700);
  }
  function longPress() {
    if (pressing) return;
    pressing = true;
    scalePhone.dataset.pressing = sideButton.dataset.pressing = 'true';
    setTimeout(() => {
      pressing = false;
      scalePhone.dataset.pressing = sideButton.dataset.pressing = 'false';
      setMode(mode === 'phone' ? 'tablet' : 'phone');
    }, reducedMotion.matches ? 0 : 450);
  }
  let autoTimer;
  let autoOn = !reducedMotion.matches;
  let visible = true;
  function schedule() {
    clearTimeout(autoTimer);
    autoButton.textContent = autoOn ? 'Pause auto-switch' : 'Play auto-switch';
    if (!autoOn || !visible || document.hidden) return;
    autoTimer = setTimeout(() => { longPress(); schedule(); }, 3800);
  }
  function stopAuto() { autoOn = false; schedule(); }
  sideButton.addEventListener('click', () => { stopAuto(); longPress(); });
  presets.forEach(button => button.addEventListener('click', () => { stopAuto(); setMode(button.dataset.modePreset); }));
  autoButton.addEventListener('click', () => { autoOn = !autoOn; schedule(); });
  document.addEventListener('visibilitychange', schedule);
  if ('IntersectionObserver' in window) new IntersectionObserver(entries => {
    visible = entries[0].isIntersecting; schedule();
  }).observe(demo);
  schedule();
}
// Hold Symbols illustration: a quick press types the letter, holding past the app's
// default 400 ms types the symbol printed on the key. Works with pointer, touch or keyboard.
const holdPhone = document.querySelector('[data-hold-phone]');
if (holdPhone) {
  const demo = holdPhone.closest('.demo');
  const typed = holdPhone.querySelector('.typed-text');
  const caret = holdPhone.querySelector('.caret');
  const bubble = holdPhone.querySelector('.hold-bubble');
  const bubbleText = holdPhone.querySelector('.hold-bubble-text');
  const status = document.querySelector('#hold-status');
  const autoButton = document.querySelector('#hold-auto');
  const clearButton = document.querySelector('#hold-clear');
  const pressButton = document.querySelector('#hold-press');
  const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
  const HOLD_MS = 400;
  const keys = {};
  for (const key of holdPhone.querySelectorAll('.elite-key')) {
    const name = key.dataset.key;
    const alt = key.querySelector('.key-alt');
    if (/^[A-Z]$/.test(name) && alt) keys[name] = { key, symbol: alt.textContent.replace('−', '-') };
    else if (name === 'space') keys[' '] = { key };
    else if (name === 'backspace') keys.Backspace = { key };
  }
  const nameOf = key => key.dataset.key === 'space' ? ' ' : key.dataset.key === 'backspace' ? 'Backspace' : key.dataset.key;
  let text = typed.textContent;
  let active = null;
  let bubbleTimer;
  function render() {
    const shown = text.length > 24 ? '…' + text.slice(-23) : text;
    typed.textContent = shown;
    let width = 0;
    try { width = shown ? typed.getComputedTextLength() : 0; } catch (_) { }
    caret.setAttribute('x', (48 + width + (shown ? 1.5 : 0)).toFixed(1));
  }
  function showBubble(entry) {
    const box = entry.key.querySelector('path').getBBox();
    bubble.setAttribute('transform', `translate(${(box.x + box.width / 2).toFixed(1)} ${box.y.toFixed(1)})`);
    bubbleText.textContent = entry.symbol;
    clearTimeout(bubbleTimer);
    holdPhone.dataset.bubble = 'true';
  }
  function press(name) {
    const entry = keys[name];
    if (!entry) return;
    if (active) release(active.name);
    entry.key.dataset.state = 'down';
    if (name === 'S') pressButton.dataset.state = 'down';
    active = { name, held: false, timer: 0 };
    const current = active;
    if (entry.symbol) current.timer = setTimeout(() => {
      if (active !== current) return;
      current.held = true;
      entry.key.dataset.state = 'held';
      if (name === 'S') pressButton.dataset.state = 'held';
      text += entry.symbol;
      render(); showBubble(entry);
      status.textContent = `Held ${name} → ${entry.symbol}`;
    }, HOLD_MS); // the hold time is the feature, not an animation: same with reduced motion
  }
  function release(name) {
    if (!active || active.name !== name) return;
    const entry = keys[name];
    clearTimeout(active.timer);
    if (!active.held) {
      if (name === 'Backspace') text = text.slice(0, -1);
      else text += name === ' ' ? ' ' : name.toLowerCase();
      render();
      status.textContent = name === ' ' ? 'Space' : name === 'Backspace' ? 'Backspace' : `Tapped ${name} → ${name.toLowerCase()}`;
    }
    delete entry.key.dataset.state;
    delete pressButton.dataset.state;
    active = null;
    clearTimeout(bubbleTimer);
    bubbleTimer = setTimeout(() => { holdPhone.dataset.bubble = 'false'; }, 450);
  }
  function cancel() {
    if (!active) return;
    clearTimeout(active.timer);
    delete keys[active.name].key.dataset.state;
    delete pressButton.dataset.state;
    active = null;
    holdPhone.dataset.bubble = 'false';
  }
  // Watch it type: plain taps, then held keys for "4:30!".
  const script = [...'see you at '].map(c => [c === ' ' ? ' ' : c.toUpperCase(), false])
    .concat([['S', true], ['H', true], ['R', true], ['Q', true], ['B', true]]);
  const wait = ms => new Promise(resolve => setTimeout(resolve, ms));
  let autoOn = !reducedMotion.matches;
  let visible = true;
  let run = 0;
  async function play(token) {
    text = ''; render();
    await wait(800);
    for (const [name, hold] of script) {
      if (token !== run) return;
      press(name);
      await wait(hold ? HOLD_MS + 320 : 120);
      if (token !== run) return;
      release(name);
      await wait(hold ? 380 : 90);
    }
    await wait(2800);
    if (token === run) play(token);
  }
  function setAuto(on) {
    autoOn = on;
    run++;
    cancel();
    autoButton.textContent = on ? 'Pause demo' : 'Play demo';
    if (on && visible && !document.hidden) play(run);
  }
  holdPhone.addEventListener('pointerdown', event => {
    const key = event.target.closest('.elite-key');
    if (!key || !keys[nameOf(key)]) return;
    event.preventDefault();
    if (autoOn) setAuto(false);
    press(nameOf(key));
  });
  const letGo = () => { if (active) release(active.name); };
  holdPhone.addEventListener('pointerup', letGo);
  holdPhone.addEventListener('pointerleave', letGo);
  holdPhone.addEventListener('pointercancel', letGo);
  holdPhone.addEventListener('contextmenu', event => event.preventDefault());
  // A real keyboard works too, while the demo is on screen and nothing else has focus.
  const typingTarget = el => el && (el.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(el.tagName));
  const keyName = event => event.key === ' ' ? ' ' : event.key === 'Backspace' ? 'Backspace' : /^[a-z]$/i.test(event.key) ? event.key.toUpperCase() : null;
  document.addEventListener('keydown', event => {
    const name = keyName(event);
    if (!name || !visible || event.ctrlKey || event.metaKey || event.altKey || typingTarget(document.activeElement)) return;
    // Space still works buttons and links elsewhere; on the demo's own controls it types a space.
    const focused = document.activeElement;
    if (name === ' ' && focused && focused.closest('button,a,summary') && !demo.contains(focused)) return;
    event.preventDefault();
    if (event.repeat) return;
    if (autoOn) setAuto(false);
    press(name);
  });
  document.addEventListener('keyup', event => { const name = keyName(event); if (name) release(name); });
  // The big button is the S key: press and hold it with a finger, mouse, Space or Enter.
  pressButton.addEventListener('pointerdown', event => {
    event.preventDefault();
    if (autoOn) setAuto(false);
    press('S');
  });
  pressButton.addEventListener('pointerup', letGo);
  pressButton.addEventListener('pointerleave', letGo);
  pressButton.addEventListener('pointercancel', letGo);
  pressButton.addEventListener('contextmenu', event => event.preventDefault());
  pressButton.addEventListener('keydown', event => {
    if (event.key !== ' ' && event.key !== 'Enter') return;
    event.preventDefault(); event.stopPropagation();
    if (event.repeat) return;
    if (autoOn) setAuto(false);
    press('S');
  });
  pressButton.addEventListener('keyup', event => {
    if (event.key === ' ' || event.key === 'Enter') { event.stopPropagation(); release('S'); }
  });
  pressButton.addEventListener('click', event => event.preventDefault());
  autoButton.addEventListener('click', () => setAuto(!autoOn));
  clearButton.addEventListener('click', () => {
    if (autoOn) setAuto(false);
    text = ''; render();
    status.textContent = 'Tap a key for the letter. Hold it for the symbol.';
  });
  document.addEventListener('visibilitychange', () => { run++; cancel(); if (autoOn && visible && !document.hidden) play(run); });
  if ('IntersectionObserver' in window) new IntersectionObserver(entries => {
    const was = visible;
    visible = entries[0].isIntersecting;
    if (visible === was) return;
    run++; cancel();
    if (autoOn && visible && !document.hidden) play(run);
  }).observe(demo);
  autoButton.textContent = autoOn ? 'Pause demo' : 'Play demo';
  render();
  if (autoOn) play(run);
}
// Optional setup progress is stored only on this browser, never submitted.
const checks = [...document.querySelectorAll('[data-step-check]')];
if (checks.length) {
  // Each guide keeps its own checklist, so resetting one never clears another.
  const key = document.body.dataset.progressKey || 'titan-tools-setup-v2';
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
// Static markup for listed tools remains complete when JavaScript or fetching is unavailable.
const catalogue = document.querySelector('#tool-catalogue');
if (catalogue) fetch('data/tools.json').then(response => {
  if (!response.ok) throw new Error('Catalogue unavailable');
  return response.json();
}).then(data => {
  if (!Array.isArray(data.tools)) return;
  for (const tool of data.tools.filter(t => t.status === 'available' && !catalogue.querySelector(`[data-tool="${t.id}"]`))) {
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
