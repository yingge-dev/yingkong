/* 颖控 · 可点原型 v0.2
   控制台 / 记录 / 工具 / 应用 / 设置
   全部为模拟数据，不会修改系统。 */

const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

const state = {
  tab: 'monitor',
  range: 'session',
  refreshMode: 'max',
  refreshLabel: '面板最高',
  currentHz: 165,
  perf: '电竞',
  charge: 80,
  thermal: 46,
  perm: 'shizuku',
  sampling: '2s',
  sessionSeconds: 18 * 60 + 42,
  filter: 'all',
  query: '',
  batt: 82,
  cpu: 42,
  gpu: 68,
  ram: 9.2
};

const REFRESH_OPTIONS = [
  { id: 'max', label: '面板最高', sub: '当前 165Hz · 换机自动跟随', tag: '推荐' },
  { id: 'match', label: '跟随游戏帧率', sub: '按游戏目标解析，60fps 游戏锁 60，省电', tag: '智能' },
  { id: '60', label: '固定 60Hz', sub: '省电优先', tag: '本机支持' },
  { id: '90', label: '固定 90Hz', sub: '平衡', tag: '本机支持' },
  { id: '120', label: '固定 120Hz', sub: '流畅', tag: '本机支持' },
  { id: '144', label: '固定 144Hz', sub: '高刷', tag: '本机支持' },
  { id: '165', label: '固定 165Hz', sub: '面板最高档 · 游戏首选', tag: '本机支持' }
];

const PERF_OPTIONS = [
  { id: '省电', label: '省电', sub: '限制大核 · 降低 GPU 频率' },
  { id: '均衡', label: '均衡', sub: '系统默认 · 日常使用' },
  { id: '性能', label: '性能', sub: '放开调度 · 优先响应' },
  { id: '电竞', label: '电竞', sub: '满血调度 · 锁最高刷新率' }
];

const CHARGE_OPTIONS = [
  { id: '60', label: '60%', sub: '最保守 · 适合长期插电' },
  { id: '70', label: '70%', sub: '偏保守' },
  { id: '80', label: '80%', sub: '推荐 · 循环寿命与续航平衡' },
  { id: '90', label: '90%', sub: '偏续航' },
  { id: '100', label: '100%', sub: '不限制' }
];

const THERMAL_OPTIONS = [
  { id: '42', label: '42°C', sub: '激进降温 · 性能损失明显' },
  { id: '44', label: '44°C', sub: '偏保守' },
  { id: '46', label: '46°C', sub: '推荐 · 平衡' },
  { id: '48', label: '48°C', sub: '偏性能 · 机身会更热' }
];

const APPS = [
  { name: '原神', pkg: 'com.miHoYo.Yuanshen', short: '原', color: '#5B8DEF', bucket: '活跃', frozen: false, note: '前台 18:42' },
  { name: '王者荣耀', pkg: 'com.tencent.tmgp.sgame', short: '王', color: '#F0A33A', bucket: '活跃', frozen: false, note: '前台 20:12' },
  { name: '和平精英', pkg: 'com.tencent.tmgp.pubgmhd', short: '和', color: '#3BA55D', bucket: '工作', frozen: false, note: '昨天 21:40' },
  { name: '抖音', pkg: 'com.ss.android.ugc.aweme', short: '抖', color: '#161823', bucket: '频繁', frozen: false, note: '后台唤醒 24 次' },
  { name: '微信', pkg: 'com.tencent.mm', short: '微', color: '#07C160', bucket: '工作', frozen: false, note: '常驻通知' },
  { name: '淘宝', pkg: 'com.taobao.taobao', short: '淘', color: '#FF5000', bucket: '受限', frozen: false, note: '后台唤醒 12 次' },
  { name: '哔哩哔哩', pkg: 'tv.danmaku.bili', short: 'B', color: '#00A1D6', bucket: '频繁', frozen: false, note: '昨天 23:10' },
  { name: '支付宝', pkg: 'com.eg.android.AlipayGphone', short: '支', color: '#1677FF', bucket: '工作', frozen: false, note: '后台唤醒 3 次' },
  { name: '小红书', pkg: 'com.xingin.xhs', short: '小', color: '#FF2442', bucket: '频繁', frozen: false, note: '后台唤醒 9 次' },
  { name: '相机', pkg: 'com.android.camera', short: '相', color: '#8E8E93', bucket: '活跃', frozen: false, note: '系统应用' }
];

const AB_DATA = {
  session: {
    before: [104, 106, 105, 103, 107, 104, 102, 105, 103, 106, 104, 102, 105, 104, 103, 106, 105, 104, 102, 105, 103, 104, 106, 105, 103, 102, 104, 105, 103, 106, 105, 104, 102, 105, 104, 103, 105, 104, 106, 105],
    after: [112, 116, 118, 119, 117, 120, 118, 119, 121, 118, 120, 119, 117, 120, 118, 119, 121, 120, 118, 117, 119, 120, 118, 119, 121, 120, 118, 119, 120, 118, 119, 121, 119, 120, 118, 119, 120, 118, 119, 118],
    delta: '+12.4',
    metrics: ['-3.2pp', '+18.2', '+1.8°C'],
    marker: '21:03 调优'
  },
  today: {
    before: [102, 104, 101, 103, 100, 104, 102, 99, 103, 101, 104, 102, 100, 103, 101, 99, 102, 104, 101, 103, 100, 102, 104, 101, 99, 103, 102, 100, 104, 101, 103, 102, 100, 103, 101, 99, 103, 102, 104, 101],
    after: [110, 114, 117, 115, 118, 116, 119, 117, 115, 118, 116, 119, 117, 115, 118, 116, 119, 117, 115, 118, 116, 119, 117, 115, 118, 116, 119, 117, 115, 118, 116, 119, 117, 115, 118, 116, 119, 117, 118, 116],
    delta: '+9.8',
    metrics: ['-2.4pp', '+14.6', '+1.2°C'],
    marker: '18:20 调优'
  },
  week: {
    before: [103, 105, 104, 106, 104, 107, 105, 103, 106, 104, 105, 103, 106, 104, 105, 107, 104, 106, 105, 103, 106, 104, 105, 103, 106, 104, 105, 107, 104, 106, 105, 103, 106, 104, 105, 103, 106, 104, 105, 104],
    after: [113, 115, 117, 116, 118, 117, 119, 116, 118, 117, 119, 118, 116, 118, 117, 119, 118, 116, 118, 117, 119, 118, 116, 118, 117, 119, 118, 116, 118, 117, 119, 118, 116, 118, 117, 119, 118, 116, 118, 117],
    delta: '+11.2',
    metrics: ['-2.9pp', '+16.1', '+1.5°C'],
    marker: '周三 调优'
  }
};

const CORE_FREQ = ['2.6G', '2.6G', '1.9G', '1.9G', '1.9G', '1.0G', '1.0G', '1.0G'];
let coreLoads = [52, 48, 44, 40, 38, 32, 30, 28];

let fpsData = Array.from({ length: 48 }, (_, i) => 116 + Math.sin(i / 4) * 2.4 + Math.random() * 2);
let tempData = Array.from({ length: 24 }, (_, i) => 37.9 + Math.sin(i / 5) * .35 + Math.random() * .15);
let powerData = Array.from({ length: 24 }, (_, i) => 5.5 + Math.sin(i / 4) * .25 + Math.random() * .12);
let cpuData = Array.from({ length: 24 }, () => 40 + Math.random() * 8);
let gpuData = Array.from({ length: 24 }, () => 62 + Math.random() * 10);
let ramData = Array.from({ length: 24 }, () => 9.1 + Math.random() * .2);
let battData = Array.from({ length: 24 }, () => 82);

/* ---------- helpers ---------- */
function haptic() {
  try { navigator.vibrate && navigator.vibrate(7); } catch (e) {}
}

function toast(msg) {
  const el = $('#toast');
  el.textContent = msg;
  el.classList.add('is-show');
  clearTimeout(toast._t);
  toast._t = setTimeout(() => el.classList.remove('is-show'), 1900);
}

function tweenNum(el, to, digits = 1) {
  if (!el) return;
  const from = parseFloat(el.textContent) || 0;
  if (Math.abs(to - from) < 0.005) { el.textContent = to.toFixed(digits); return; }
  const t0 = performance.now(), dur = 380;
  function frame(t) {
    const p = Math.min(1, (t - t0) / dur);
    const e = 1 - Math.pow(1 - p, 3);
    el.textContent = (from + (to - from) * e).toFixed(digits);
    if (p < 1) requestAnimationFrame(frame);
  }
  requestAnimationFrame(frame);
}

function hexAlpha(hex, alpha) {
  const h = hex.replace('#', '');
  const n = parseInt(h, 16);
  return `rgba(${(n >> 16) & 255},${(n >> 8) & 255},${n & 255},${alpha})`;
}

function smoothPath(pts) {
  if (!pts.length) return '';
  if (pts.length === 1) return `M${pts[0].x},${pts[0].y}`;
  let d = `M${pts[0].x.toFixed(2)},${pts[0].y.toFixed(2)}`;
  for (let i = 0; i < pts.length - 1; i++) {
    const p0 = pts[i - 1] || pts[i];
    const p1 = pts[i];
    const p2 = pts[i + 1];
    const p3 = pts[i + 2] || p2;
    const c1x = p1.x + (p2.x - p0.x) / 6;
    const c1y = p1.y + (p2.y - p0.y) / 6;
    const c2x = p2.x - (p3.x - p1.x) / 6;
    const c2y = p2.y - (p3.y - p1.y) / 6;
    d += ` C${c1x.toFixed(2)},${c1y.toFixed(2)} ${c2x.toFixed(2)},${c2y.toFixed(2)} ${p2.x.toFixed(2)},${p2.y.toFixed(2)}`;
  }
  return d;
}

function chartWidth(el) {
  const w = el.clientWidth;
  return w > 40 ? w : 326;
}

/* ---------- charts ---------- */
function drawFpsChart() {
  const el = $('#fpsChart');
  if (!el) return;
  const w = chartWidth(el), h = el.clientHeight || 58;
  const min = 102, max = 124;
  const pts = fpsData.map((v, i) => ({
    x: 2 + (i / (fpsData.length - 1)) * (w - 4),
    y: 4 + (1 - (Math.max(min, Math.min(max, v)) - min) / (max - min)) * (h - 8)
  }));
  const d = smoothPath(pts);
  const area = `${d} L${(w - 2).toFixed(2)},${h} L2,${h} Z`;
  const last = pts[pts.length - 1];
  el.innerHTML = `
    <svg viewBox="0 0 ${w} ${h}" width="${w}" height="${h}" preserveAspectRatio="none">
      <defs>
        <linearGradient id="gFps" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="#FF6900" stop-opacity=".18"/>
          <stop offset="1" stop-color="#FF6900" stop-opacity="0"/>
        </linearGradient>
      </defs>
      <path d="${area}" fill="url(#gFps)"/>
      <path d="${d}" fill="none" stroke="#FF6900" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      <circle cx="${last.x.toFixed(1)}" cy="${last.y.toFixed(1)}" r="3.2" fill="#fff" stroke="#FF6900" stroke-width="2"/>
    </svg>`;
}

function smoothSeries(arr) {
  let out = arr.slice();
  for (let pass = 0; pass < 2; pass++) {
    out = out.map((v, i) => {
      const a = out[Math.max(0, i - 1)], c = out[Math.min(out.length - 1, i + 1)];
      return (a + v * 2 + c) / 4;
    });
  }
  return out;
}

function drawABChart() {
  const el = $('#abChart');
  if (!el) return;
  const data = AB_DATA[state.range];
  const beforeRaw = smoothSeries(data.before);
  const afterRaw = smoothSeries(data.after);
  const w = chartWidth(el), h = el.clientHeight || 152;
  const left = 22, right = 8, top = 10, bottom = 12;
  const plotW = w - left - right;
  const min = 55, max = 126;
  const x = i => left + (i / (afterRaw.length - 1)) * plotW;
  const y = v => top + (1 - (v - min) / (max - min)) * (h - top - bottom);

  const afterPts = afterRaw.map((v, i) => ({ x: x(i), y: y(v) }));
  const beforePts = beforeRaw.map((v, i) => ({ x: x(i), y: y(v) }));
  const afterD = smoothPath(afterPts);
  const beforeD = smoothPath(beforePts);
  const area = `${afterD} L${x(afterRaw.length - 1).toFixed(1)},${(h - bottom).toFixed(1)} L${left},${(h - bottom).toFixed(1)} Z`;

  const gridSvg = [60, 90, 120].map(g => `
    <line x1="${left}" y1="${y(g).toFixed(1)}" x2="${w - right}" y2="${y(g).toFixed(1)}" stroke="rgba(60,60,67,.08)" stroke-width="1"/>
    <text x="0" y="${(y(g) + 3).toFixed(1)}" fill="#B0B0B5" font-family="Inter, sans-serif" font-size="8">${g}</text>`).join('');

  const mi = Math.floor(afterRaw.length * 0.55);
  const mx = x(mi), my = y(afterRaw[mi]);
  const last = afterPts[afterPts.length - 1];
  const lastBefore = beforePts[beforePts.length - 1];

  el.innerHTML = `
    <svg viewBox="0 0 ${w} ${h}" width="${w}" height="${h}">
      <defs>
        <linearGradient id="gAB" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="#FF6900" stop-opacity=".13"/>
          <stop offset="1" stop-color="#FF6900" stop-opacity="0"/>
        </linearGradient>
      </defs>
      ${gridSvg}
      <path d="${area}" fill="url(#gAB)"/>
      <path d="${beforeD}" fill="none" stroke="#B9B9BE" stroke-width="1.6" stroke-dasharray="4 4" stroke-linecap="round"/>
      <path d="${afterD}" fill="none" stroke="#FF6900" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
      <line x1="${mx.toFixed(1)}" y1="${top}" x2="${mx.toFixed(1)}" y2="${(h - bottom).toFixed(1)}" stroke="rgba(255,105,0,.28)" stroke-width="1" stroke-dasharray="3 3"/>
      <circle cx="${mx.toFixed(1)}" cy="${my.toFixed(1)}" r="3" fill="#fff" stroke="#FF6900" stroke-width="2"/>
      <circle cx="${last.x.toFixed(1)}" cy="${last.y.toFixed(1)}" r="3" fill="#fff" stroke="#FF6900" stroke-width="2"/>
      <text x="${(mx + 6).toFixed(1)}" y="${(top + 2).toFixed(1)}" fill="#FF6900" font-family="Inter, sans-serif" font-size="8">${data.marker}</text>
      <text x="${(w - right).toFixed(1)}" y="${(last.y - 8).toFixed(1)}" text-anchor="end" fill="#FF6900" font-family="Inter, sans-serif" font-size="8.5" font-weight="600">${afterRaw[afterRaw.length - 1].toFixed(1)}</text>
      <text x="${(w - right).toFixed(1)}" y="${(lastBefore.y + 12).toFixed(1)}" text-anchor="end" fill="#A9A9AE" font-family="Inter, sans-serif" font-size="8">${beforeRaw[beforeRaw.length - 1].toFixed(1)}</text>
    </svg>`;
}

function drawMini(el, data, color, min, max) {
  if (!el) return;
  const w = el.clientWidth > 30 ? el.clientWidth : 140;
  const h = el.clientHeight || 16;
  const pts = data.map((v, i) => ({
    x: 1 + (i / (data.length - 1)) * (w - 2),
    y: 1.5 + (1 - (Math.max(min, Math.min(max, v)) - min) / (max - min)) * (h - 3)
  }));
  const d = smoothPath(pts);
  el.innerHTML = `<svg viewBox="0 0 ${w} ${h}" width="${w}" height="${h}"><path d="${d}" fill="none" stroke="${color}" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
}

function drawTimeline() {
  drawMini($('#tlFps'), fpsData.slice(-24), '#FF6900', 96, 124);
  drawMini($('#tlTemp'), tempData, '#FFB27A', 36, 40);
  drawMini($('#tlPower'), powerData, '#0A84FF', 5, 6.2);
  if ($('#tlFpsVal')) $('#tlFpsVal').textContent = fpsData[fpsData.length - 1].toFixed(1);
  if ($('#tlTempVal')) $('#tlTempVal').textContent = tempData[tempData.length - 1].toFixed(1) + '°';
  if ($('#tlPowerVal')) $('#tlPowerVal').textContent = powerData[powerData.length - 1].toFixed(2) + 'W';
}

function drawConsole() {
  tweenNum($('#vCpu'), state.cpu, 0);
  tweenNum($('#vGpu'), state.gpu, 0);
  tweenNum($('#vRam'), state.ram, 1);
  tweenNum($('#vTemp'), tempData[tempData.length - 1], 1);
  tweenNum($('#vPower'), powerData[powerData.length - 1], 2);
  tweenNum($('#vBatt'), state.batt, 0);
  drawMini($('#cCpu'), cpuData, '#AF52DE', 28, 60);
  drawMini($('#cGpu'), gpuData, '#0A84FF', 45, 88);
  drawMini($('#cRam'), ramData, '#30B0C7', 8.6, 10);
  drawMini($('#cTemp'), tempData, '#FF6900', 36, 42);
  drawMini($('#cPower'), powerData, '#FFB340', 5, 6.4);
  drawMini($('#cBatt'), battData, '#34C759', 78, 86);
}

function initCores() {
  const grid = $('#coreGrid');
  if (!grid) return;
  grid.innerHTML = CORE_FREQ.map((f, i) => `
    <div class="core">
      <span>${i}</span>
      <div class="cbar"><i id="coreBar${i}" style="height:${coreLoads[i]}%"></i></div>
      <b>${f}</b>
    </div>`).join('');
}

function drawCores() {
  coreLoads.forEach((v, i) => {
    const bar = $('#coreBar' + i);
    if (bar) bar.style.height = Math.max(6, Math.min(100, v)) + '%';
  });
}

/* ---------- monitor tick ---------- */
function tickMonitor() {
  const base = fpsData[fpsData.length - 1];
  let next = base + (Math.random() - 0.5) * 2.2;
  if (Math.random() < 0.06) next = 96 + Math.random() * 10;
  next = Math.max(92, Math.min(122, next));
  fpsData.push(next);
  if (fpsData.length > 48) fpsData.shift();
  const fps = fpsData[fpsData.length - 1];
  tweenNum($('#fps'), fps, 1);
  tweenNum($('#low'), Math.max(88, Math.min(...fpsData.slice(-20)) - 3 + Math.random()), 1);
  tweenNum($('#jank'), 1.2 + Math.random() * 1.2, 1);

  const temp = 38.4 + Math.sin(Date.now() / 90000) * 0.7 + Math.random() * 0.3;
  tempData.push(temp); if (tempData.length > 24) tempData.shift();
  const power = 5.6 + Math.random() * 0.5;
  powerData.push(power); if (powerData.length > 24) powerData.shift();

  state.cpu = Math.round(38 + Math.random() * 12);
  state.gpu = Math.round(62 + Math.random() * 12);
  state.ram = 9.1 + Math.random() * 0.3;
  state.batt = Math.min(99, state.batt + 0.01);

  cpuData.push(state.cpu); if (cpuData.length > 24) cpuData.shift();
  gpuData.push(state.gpu); if (gpuData.length > 24) gpuData.shift();
  ramData.push(state.ram); if (ramData.length > 24) ramData.shift();
  battData.push(state.batt); if (battData.length > 24) battData.shift();

  coreLoads = coreLoads.map(v => Math.max(6, Math.min(96, v + (Math.random() - 0.5) * 14)));

  state.sessionSeconds += 1;
  const mm = String(Math.floor(state.sessionSeconds / 60)).padStart(2, '0');
  const ss = String(state.sessionSeconds % 60).padStart(2, '0');
  if ($('#session')) $('#session').textContent = `本局 ${mm}:${ss}`;

  if (state.tab === 'monitor') {
    drawFpsChart(); drawConsole(); drawCores(); drawTimeline();
  }
}

/* ---------- rendering ---------- */
function updateVerify() {
  if ($('#verifyChip')) $('#verifyChip').textContent = `${state.currentHz}Hz 已核验`;
  if ($('#refreshValue')) $('#refreshValue').textContent = state.refreshLabel;
  if ($('#refreshSub')) {
    $('#refreshSub').textContent = state.refreshMode === 'max'
      ? `面板最高 · 当前 ${state.currentHz}Hz`
      : state.refreshMode === 'match'
        ? `跟随帧率 · 按游戏目标`
        : `固定档 · 已锁定 ${state.currentHz}Hz`;
  }
  if ($('#perfVal')) $('#perfVal').textContent = state.perf;
  if ($('#chargeVal')) $('#chargeVal').textContent = state.charge + '%';
  if ($('#thermalVal')) $('#thermalVal').textContent = state.thermal + '°C';
}

function renderApps() {
  const list = $('#appList');
  if (!list) return;
  const q = state.query.trim().toLowerCase();
  const items = APPS.filter(a => {
    if (state.filter === 'frozen' && !a.frozen) return false;
    if (state.filter === 'limited' && a.bucket !== '受限') return false;
    if (q && !(a.name.toLowerCase().includes(q) || a.pkg.toLowerCase().includes(q))) return false;
    return true;
  });
  if (!items.length) {
    list.innerHTML = '<div class="empty">没有匹配的应用</div>';
    return;
  }
  list.innerHTML = items.map(a => {
    const badge = a.frozen ? '<span class="badge frozen">已冻结</span>'
      : a.bucket === '活跃' ? '<span class="badge active">活跃</span>'
      : a.bucket === '受限' ? '<span class="badge limited">受限</span>'
      : `<span class="badge">${a.bucket}</span>`;
    return `<button class="app-row ${a.frozen ? 'is-frozen' : ''}" data-pkg="${a.pkg}">
      <span class="app-icon" style="background:${hexAlpha(a.color, .12)};color:${a.color}">${a.short}</span>
      <span class="app-meta"><b>${a.name}</b><em>${a.pkg} · ${a.note}</em></span>
      ${badge}
    </button>`;
  }).join('');
}

function renderRange() {
  const d = AB_DATA[state.range];
  $$('#rangeSeg button').forEach(b => b.classList.toggle('is-on', b.dataset.range === state.range));
  const big = $('.delta-big b');
  if (big) big.textContent = d.delta;
  $$('.compare b').forEach((b, i) => { b.textContent = d.metrics[i]; });
  drawABChart();
}

function switchTab(name) {
  if (!$('#screen-' + name)) return;
  state.tab = name;
  $$('.screen').forEach(s => s.classList.toggle('is-active', s.id === 'screen-' + name));
  $$('.tab').forEach(t => t.classList.toggle('is-on', t.dataset.tab === name));
  const content = $('#content');
  if (content) content.scrollTop = 0;
  try { history.replaceState(null, '', '#' + name); } catch (e) {}
  if (name === 'monitor') requestAnimationFrame(() => { drawFpsChart(); drawConsole(); drawCores(); drawTimeline(); });
  if (name === 'records') requestAnimationFrame(drawABChart);
}

/* ---------- sheets ---------- */
function openSheet(title, html) {
  $('#sheetTitle').textContent = title;
  $('#sheetBody').innerHTML = html;
  $('#scrim').classList.add('is-open');
  $('#sheet').classList.add('is-open');
}

function closeSheet() {
  $('#scrim').classList.remove('is-open');
  $('#sheet').classList.remove('is-open');
}

function optionsHtml(items, selected, attr) {
  return items.map(o => {
    const on = String(o.id) === String(selected);
    return `<button class="option ${on ? 'is-on' : ''}" data-${attr}="${o.id}">
      <span class="option-radio"></span>
      <span class="option-text"><b>${o.label}</b><em>${o.sub || ''}</em></span>
      <span class="option-tag">${o.tag || ''}</span>
    </button>`;
  }).join('');
}

function refreshSheet() {
  openSheet('刷新率', `<div class="option-list">${optionsHtml(REFRESH_OPTIONS, state.refreshMode, 'refresh')}</div>
    <p class="sheet-note">选项来自本机真实档位，不存在的档位不会出现。选完后会写入并回读，确认生效才显示「已核验」。</p>`);
}

function perfSheet() {
  openSheet('性能档', `<div class="option-list">${optionsHtml(PERF_OPTIONS, state.perf, 'perf')}</div>
    <p class="sheet-note">CPU 调度器、GPU 调频、IO 与 VM 参数会一起切换；写入前备份，失败自动还原。</p>`);
}

function chargeSheet() {
  openSheet('充电上限', `<div class="option-list">${optionsHtml(CHARGE_OPTIONS, state.charge, 'charge')}</div>
    <p class="sheet-note">需要 Root 写充电节点；不支持该节点的机型会隐藏此页。</p>`);
}

function thermalSheet() {
  openSheet('温控墙', `<div class="option-list">${optionsHtml(THERMAL_OPTIONS, state.thermal, 'thermal')}</div>
    <p class="sheet-note">同时修改温度墙与降频策略，写入后回读 thermal_zone 确认生效；需要 Root。</p>`);
}

function chooseRefresh(id) {
  state.refreshMode = id;
  if (id === 'max') { state.refreshLabel = '面板最高'; state.currentHz = 165; }
  else if (id === 'match') { state.refreshLabel = '跟随帧率'; state.currentHz = 120; }
  else { state.refreshLabel = `固定 ${id}Hz`; state.currentHz = Number(id); }
  updateVerify();
  closeSheet();
  haptic();
  toast(`${state.refreshLabel} · 已核验`);
}

function appSheet(pkg) {
  const a = APPS.find(x => x.pkg === pkg);
  if (!a) return;
  openSheet(a.name, `
    <div class="option-list">
      <div class="option" style="border-top:0">
        <span class="app-icon" style="background:${hexAlpha(a.color, .12)};color:${a.color};width:34px;height:34px">${a.short}</span>
        <span class="option-text"><b>${a.pkg}</b><em>当前分组：${a.bucket}${a.frozen ? ' · 已冻结' : ''}</em></span>
      </div>
    </div>
    <button class="action-btn primary" data-app-action="freeze">${a.frozen ? '解冻应用' : '冻结应用'}</button>
    <button class="action-btn" data-app-action="bucket">切换待机分组（当前 ${a.bucket}）</button>
    <button class="action-btn" data-app-action="stop">强制停止</button>
    <button class="action-btn" data-app-action="info">应用信息</button>
    <p class="sheet-note">冻结与待机分组通过 Shizuku 执行；所有操作可撤回，原型中不会真的改动系统。</p>`);
  $('#sheetBody').dataset.pkg = pkg;
}

/* ---------- module sheets ---------- */
function cpuSheet() {
  const clusters = [
    { name: '大核 · 2 核', min: 800, max: 2600, val: 2600, gov: 'schedutil' },
    { name: '中核 · 2 核', min: 600, max: 2200, val: 1900, gov: 'schedutil' },
    { name: '小核 · 4 核', min: 400, max: 1800, val: 1000, gov: 'schedutil' }
  ];
  openSheet('CPU 控制', clusters.map(c => `
    <div class="cluster">
      <div class="cluster-head">${c.name}<span class="range-val">${c.val} MHz</span></div>
      <input class="range" type="range" min="${c.min}" max="${c.max}" value="${c.val}"
        oninput="this.previousElementSibling.querySelector('.range-val').textContent=this.value+' MHz'">
      <div class="range-meta"><span>${c.min} MHz</span><span>${c.max} MHz · ${c.gov}</span></div>
    </div>`).join('') + `<p class="sheet-note">写前备份原频率；需要 Root。写入后回读 cpufreq 确认生效，失败自动还原。</p>`);
}

function processSheet() {
  const rows = [
    ['原神', '8470', '2.4G', '68.7%'],
    ['system_server', '1234', '1.8G', '12.3%'],
    ['surfaceflinger', '890', '420M', '8.1%'],
    ['微信', '5621', '980M', '4.2%'],
    ['抖音', '7312', '760M', '3.1%'],
    ['王者荣耀', '9120', '2.1G', '2.8%'],
    ['淘宝', '4380', '640M', '1.2%'],
    ['哔哩哔哩', '6620', '520M', '0.8%']
  ];
  openSheet('进程管理', `
    <div class="table-head"><span>应用</span><span>PID</span><span>内存</span><span>CPU</span></div>
    ${rows.map(r => `<div class="table-row"><b>${r[0]}</b><span>${r[1]}</span><span>${r[2]}</span><span>${r[3]}</span></div>`).join('')}
    <p class="sheet-note">共 126 个进程 · 按 CPU 排序 · 数据来自 /proc 与 dumpsys。</p>`);
}

function fpsSheet() {
  const data = [112, 116, 118, 119, 117, 120, 118, 119, 121, 118, 120, 119, 117, 120, 118, 119, 121, 120, 118, 117, 119, 120, 118, 119, 121, 120, 118, 119, 120, 118, 119, 121, 119, 120, 118, 119, 120, 118, 119, 118];
  openSheet('帧率记录', `
    <div class="detail-stats">
      <div><em>平均</em><b>121.0</b><span> fps</span></div>
      <div><em>1% LOW</em><b>116.0</b><span> fps</span></div>
      <div><em>抖动</em><b>0.8</b><span> ms</span></div>
    </div>
    <div class="sheet-chart" id="sheetFpsChart"></div>
    <div class="scenario-row"><b>省电模式</b><span>118.0 fps · 4.2W</span><em>32°C · 12m45s</em></div>
    <div class="scenario-row"><b>均衡</b><span>120.0 fps · 4.8W</span><em>33°C · 1h12m</em></div>
    <div class="scenario-row"><b>性能</b><span>121.0 fps · 5.6W</span><em>35°C · 2h06m</em></div>
    <div class="scenario-row"><b>电竞</b><span>121.0 fps · 6.1W</span><em>38°C · 1h24m</em></div>
    <p class="sheet-note">场景来自前台应用 + 性能档 + 刷新率的组合；每场结束自动归档。</p>`);
  setTimeout(() => drawMini($('#sheetFpsChart'), data, '#FF6900', 100, 126), 30);
}

function powerSheet() {
  const rows = [
    ['原神', '5.8W', '32%'],
    ['抖音', '1.2W', '18%'],
    ['微信', '0.6W', '12%'],
    ['王者荣耀', '4.9W', '9%'],
    ['系统', '0.4W', '7%']
  ];
  openSheet('耗电统计', `
    <div class="detail-stats">
      <div><em>平均功率</em><b>2.01</b><span> W</span></div>
      <div><em>亮屏</em><b>1h24m</b><span></span></div>
      <div><em>待机</em><b>9h19m</b><span></span></div>
    </div>
    <div class="scene-list" style="margin-top:10px">
      ${rows.map(r => `<div class="scenario-row"><b>${r[0]}</b><span>${r[1]}</span><em>${r[2]}</em></div>`).join('')}
    </div>
    <p class="sheet-note">按应用 / 前后台 / 使用场景统计；数据留在本机，可导出 CSV。</p>`);
}

function chargeStatSheet() {
  openSheet('充电统计', `
    <div class="detail-stats">
      <div><em>当前功率</em><b>34</b><span> W</span></div>
      <div><em>电压 / 电流</em><b>12.4</b><span> V · 2.7A</span></div>
      <div><em>预计到 80%</em><b>28</b><span> min</span></div>
    </div>
    <div class="scenario-row" style="margin-top:10px"><b>充电协议</b><span>Mi Turbo Charge</span><em>34W</em></div>
    <div class="scenario-row"><b>电池温度</b><span>34.1°C</span><em>正常</em></div>
    <div class="scenario-row"><b>循环次数</b><span>312 次</span><em>健康 94%</em></div>
    <div class="scenario-row"><b>本次充入</b><span>62% → 82%</span><em>18 分钟</em></div>
    <p class="sheet-note">功率曲线、协议与温度全程留档；到上限自动断充。</p>`);
}

function activitySheet() {
  const rows = [
    ['微信', 'com.tencent.mm', '常驻 · 通知'],
    ['淘宝', 'com.taobao.taobao', '12 次唤醒'],
    ['抖音', 'com.ss.android.ugc.aweme', '24 次唤醒'],
    ['支付宝', 'com.eg.android.AlipayGphone', '3 次唤醒'],
    ['哔哩哔哩', 'tv.danmaku.bili', '后台服务 2 个']
  ];
  openSheet('活动与服务', `
    <div class="table-head" style="grid-template-columns:70px 1fr 78px"><span>应用</span><span>包名</span><span>状态</span></div>
    ${rows.map(r => `<div class="table-row" style="grid-template-columns:70px 1fr 78px"><b>${r[0]}</b><span style="text-align:left;color:var(--tertiary)">${r[1]}</span><span>${r[2]}</span></div>`).join('')}
    <p class="sheet-note">展开后可看 Activity / Service / Receiver / 闹钟；支持按应用停止。</p>`);
}

function infoSheet(kind) {
  const info = {
    swap: ['SWAP 设置', 'ZRAM 1.2G · zstd 压缩', '调整 ZRAM 大小与压缩算法，需要 Root；写入前备份 /proc/swaps。'],
    bias: ['应用偏见', '8 个应用已设置', '给单个应用设置 CPU 亲和、调度偏向和后台限制，减少抢资源。'],
    magisk: ['Magisk 模块', '12 个模块 · 2 个可更新', '列出 Magisk / KernelSU 模块，支持启用、禁用和卸载（重启生效）。'],
    addon: ['附加模块', '4 个脚本', '附加功能与自定义脚本，按开关执行，可随时停用。'],
    orms: ['ORMS 配置', '未启用', '调整渲染分辨率与渲染倍率，需要 Root；改前会记录当前分辨率。'],
    detect: ['检测工具', '屏幕 / 触控 / 传感器', '屏幕坏点、触控采样、传感器与振动检测，结果可导出。'],
    backup: ['备份还原', '2 个备份 · 最近 21:00', '备份节点值、配置和模块列表；一键还原，卸载前自动备份。'],
    nodes: ['节点编辑', '18 项 · 已备份', '自定义 /sys 与 /proc 路径，写前记录原值；只还原自己改过的项。'],
    auto: ['自动化', '2 条 · 生效', '按应用、温度、电量、充电状态触发配方，带滞回，避免来回切档。'],
    export: ['导出与分享', 'CSV / JSON / 长图', '导出采样数据、对比长图和节点清单；数据只从本机生成。']
  }[kind] || ['模块', '开发中', '该模块正在接入。'];
  openSheet(info[0], `
    <div class="detail-stats" style="grid-template-columns:1fr">
      <div><em>当前状态</em><b style="font-size:14px">${info[1]}</b></div>
    </div>
    <p class="sheet-note">${info[2]}</p>
    <button class="action-btn primary" data-toast="${info[0]}将在后续版本接入">进入模块</button>`);
}

function moduleSheet(kind) {
  if (kind === 'cpu') return cpuSheet();
  if (kind === 'process') return processSheet();
  if (kind === 'fps') return fpsSheet();
  if (kind === 'powerstat') return powerSheet();
  if (kind === 'chargestat') return chargeStatSheet();
  if (kind === 'activity') return activitySheet();
  return infoSheet(kind);
}

/* ---------- events ---------- */
document.addEventListener('click', e => {
  const tabBtn = e.target.closest('[data-tab]');
  if (tabBtn) { haptic(); switchTab(tabBtn.dataset.tab); return; }

  const goto = e.target.closest('[data-goto]');
  if (goto) { haptic(); switchTab(goto.dataset.goto); return; }

  const toastBtn = e.target.closest('[data-toast]');
  if (toastBtn) { toast(toastBtn.dataset.toast); return; }

  if (e.target.id === 'scrim' || e.target.closest('#sheetClose')) { closeSheet(); return; }

  const sheetBtn = e.target.closest('[data-sheet]');
  if (sheetBtn) {
    haptic();
    const kind = sheetBtn.dataset.sheet;
    if (kind === 'perf') perfSheet();
    if (kind === 'charge') chargeSheet();
    if (kind === 'thermal') thermalSheet();
    return;
  }

  if (e.target.closest('#refreshRow')) { haptic(); refreshSheet(); return; }

  const refreshOpt = e.target.closest('[data-refresh]');
  if (refreshOpt) { chooseRefresh(refreshOpt.dataset.refresh); return; }

  const perfOpt = e.target.closest('[data-perf]');
  if (perfOpt) {
    state.perf = perfOpt.dataset.perf;
    updateVerify(); closeSheet(); haptic(); toast(`性能档：${state.perf} · 已核验`);
    return;
  }

  const chargeOpt = e.target.closest('[data-charge]');
  if (chargeOpt) {
    state.charge = Number(chargeOpt.dataset.charge);
    updateVerify(); closeSheet(); haptic(); toast(`充电上限 ${state.charge}% · 已核验`);
    return;
  }

  const thermalOpt = e.target.closest('[data-thermal]');
  if (thermalOpt) {
    state.thermal = Number(thermalOpt.dataset.thermal);
    updateVerify(); closeSheet(); haptic(); toast(`温控墙 ${state.thermal}°C · 已核验`);
    return;
  }

  const moduleBtn = e.target.closest('[data-module]');
  if (moduleBtn) { haptic(); moduleSheet(moduleBtn.dataset.module); return; }

  const tool = e.target.closest('[data-tool]');
  if (tool) {
    haptic();
    const kind = tool.dataset.tool;
    if (kind === 'fps') { switchTab('records'); return; }
    if (kind === 'activity') { switchTab('apps'); return; }
    const msg = {
      power: '耗电账本将在 M3 接入',
      nodes: '节点编辑将在 M4 接入',
      auto: '自动化将在 M5 接入',
      export: '导出将在 M3 接入'
    }[kind] || '该模块正在开发';
    toast(msg);
    return;
  }

  const modeChip = e.target.closest('#modeChips button');
  if (modeChip) {
    state.refreshMode = modeChip.dataset.mode;
    state.refreshLabel = `固定 ${modeChip.dataset.mode}Hz`;
    state.currentHz = Number(modeChip.dataset.mode);
    updateVerify(); haptic(); toast(`${state.refreshLabel} · 已核验`);
    return;
  }

  const rangeBtn = e.target.closest('#rangeSeg button');
  if (rangeBtn) { state.range = rangeBtn.dataset.range; renderRange(); haptic(); return; }

  const filterBtn = e.target.closest('#appFilters button');
  if (filterBtn) {
    state.filter = filterBtn.dataset.filter;
    $$('#appFilters button').forEach(b => b.classList.toggle('is-on', b === filterBtn));
    renderApps(); haptic(); return;
  }

  const appRow = e.target.closest('.app-row');
  if (appRow) { haptic(); appSheet(appRow.dataset.pkg); return; }

  const appAction = e.target.closest('[data-app-action]');
  if (appAction) {
    const pkg = $('#sheetBody').dataset.pkg;
    const a = APPS.find(x => x.pkg === pkg);
    if (!a) return;
    const act = appAction.dataset.appAction;
    if (act === 'freeze') {
      a.frozen = !a.frozen;
      toast(a.frozen ? `已冻结 ${a.name}` : `已解冻 ${a.name}`);
    } else if (act === 'bucket') {
      const order = ['活跃', '工作', '频繁', '受限'];
      a.bucket = order[(order.indexOf(a.bucket) + 1) % order.length];
      toast(`${a.name} → ${a.bucket}`);
    } else if (act === 'stop') {
      toast(`已强制停止 ${a.name}（原型）`);
    } else {
      toast('应用信息页将在 M5 接入');
    }
    closeSheet(); renderApps(); haptic(); return;
  }

  const permBtn = e.target.closest('#permSwitch button');
  if (permBtn) {
    state.perm = permBtn.dataset.perm;
    $$('#permSwitch button').forEach(b => b.classList.toggle('is-on', b === permBtn));
    const head = $('#permState') || $('.perm-card .verified');
    const notes = {
      basic: ['基础模式', '基础模式只能读取公开数据，不需要 Shizuku 或 Root；部分系统级数据会被隐藏。'],
      shizuku: ['Shizuku 已连接', 'Shizuku 可读取系统数据、执行部分命令；写内核节点仍需 Root。'],
      root: ['Root 已授权', 'Root 模式可写内核节点，能力最全；每次写入都会备份并回读，失败自动还原。']
    };
    head.textContent = notes[state.perm][0];
    $('#permNote').textContent = notes[state.perm][1];
    toast(`权限模式：${permBtn.textContent}`);
    haptic(); return;
  }

  const sampleBtn = e.target.closest('#sampleSeg button');
  if (sampleBtn) {
    state.sampling = sampleBtn.textContent;
    $$('#sampleSeg button').forEach(b => b.classList.toggle('is-on', b === sampleBtn));
    toast(`采样频率 ${state.sampling}`); haptic(); return;
  }

  if (e.target.closest('#exportBtn')) { toast('已生成对比长图（原型）'); haptic(); return; }

  if (e.target.closest('#rescueRow')) {
    openSheet('救援模式', `
      <p class="sheet-note" style="margin-top:0;font-size:10.5px">将把颖控写过的节点逐项还原为备份值，并停止所有配方。不会影响其他模块。</p>
      <button class="action-btn primary" id="rescueConfirm">确认还原（12 项）</button>
      <button class="action-btn" id="rescueCancel">取消</button>`);
    setTimeout(() => {
      const ok = $('#rescueConfirm');
      const cancel = $('#rescueCancel');
      if (ok) ok.addEventListener('click', () => { closeSheet(); toast('已还原 12 个节点（原型）'); haptic(); });
      if (cancel) cancel.addEventListener('click', closeSheet);
    }, 0);
    return;
  }

  const sw = e.target.closest('.switch');
  if (sw) { sw.classList.toggle('is-on'); haptic(); }
});

const appSearch = $('#appSearch');
if (appSearch) appSearch.addEventListener('input', e => {
  state.query = e.target.value;
  renderApps();
});

document.addEventListener('keydown', e => { if (e.key === 'Escape') closeSheet(); });

let resizeTimer;
window.addEventListener('resize', () => {
  clearTimeout(resizeTimer);
  resizeTimer = setTimeout(() => { drawFpsChart(); drawConsole(); drawCores(); drawTimeline(); drawABChart(); }, 120);
});

if (document.fonts && document.fonts.ready) {
  document.fonts.ready.then(() => requestAnimationFrame(() => {
    drawFpsChart(); drawConsole(); drawCores(); drawTimeline(); drawABChart();
  }));
}

/* ---------- init ---------- */
function init() {
  initCores();
  const hash = (location.hash || '').replace('#', '');
  if (['monitor', 'records', 'tools', 'apps', 'settings'].includes(hash)) switchTab(hash);
  updateVerify();
  renderApps();
  renderRange();
  requestAnimationFrame(() => { drawFpsChart(); drawConsole(); drawCores(); drawTimeline(); drawABChart(); });
  const debugSheet = new URLSearchParams(location.search).get('sheet');
  if (debugSheet) {
    document.documentElement.classList.add('no-anim');
    moduleSheet(debugSheet);
  }
  setInterval(() => {
    if (document.hidden) return;
    tickMonitor();
  }, 1000);
}

init();
