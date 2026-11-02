/* 颖控 · 可点原型 v0.1
   纯前端模拟数据，不会修改任何系统设置。 */

const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

const state = {
  tab: 'monitor',
  range: 'session',
  refreshMode: 'max',
  refreshLabel: '面板最高',
  currentHz: 165,
  perf: '电竞',
  recipeOn: true,
  chargeOn: true,
  perm: 'shizuku',
  sampling: '2s',
  sessionSeconds: 18 * 60 + 42,
  filter: 'all',
  query: ''
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

let fpsData = Array.from({ length: 48 }, (_, i) => 116 + Math.sin(i / 4) * 2.4 + Math.random() * 2);
let tempData = Array.from({ length: 24 }, (_, i) => 37.9 + Math.sin(i / 5) * .35 + Math.random() * .15);
let powerData = Array.from({ length: 24 }, (_, i) => 5.5 + Math.sin(i / 4) * .25 + Math.random() * .12);

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
  const from = parseFloat(el.textContent) || 0;
  if (Math.abs(to - from) < 0.005) { el.textContent = to.toFixed(digits); return; }
  const t0 = performance.now(), dur = 420;
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
  const r = (n >> 16) & 255, g = (n >> 8) & 255, b = n & 255;
  return `rgba(${r},${g},${b},${alpha})`;
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
  const w = chartWidth(el), h = el.clientHeight || 62;
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
          <stop offset="0" stop-color="#FF6900" stop-opacity=".20"/>
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
  const w = chartWidth(el), h = el.clientHeight || 168;
  const left = 22, right = 8, top = 10, bottom = 12;
  const plotW = w - left - right, plotH = h - top - bottom;
  const min = 55, max = 126;
  const x = i => left + (i / (afterRaw.length - 1)) * plotW;
  const y = v => top + (1 - (v - min) / (max - min)) * plotH;

  const afterPts = afterRaw.map((v, i) => ({ x: x(i), y: y(v) }));
  const beforePts = beforeRaw.map((v, i) => ({ x: x(i), y: y(v) }));
  const afterD = smoothPath(afterPts);
  const beforeD = smoothPath(beforePts);
  const area = `${afterD} L${x(afterRaw.length - 1).toFixed(1)},${(h - bottom).toFixed(1)} L${left},${(h - bottom).toFixed(1)} Z`;

  const grids = [60, 90, 120];
  const gridSvg = grids.map(g => `
    <line x1="${left}" y1="${y(g).toFixed(1)}" x2="${w - right}" y2="${y(g).toFixed(1)}" stroke="rgba(60,60,67,.08)" stroke-width="1"/>
    <text x="0" y="${(y(g) + 3).toFixed(1)}" fill="#B0B0B5" font-family="Inter, sans-serif" font-size="8">${g}</text>`).join('');

  const mi = Math.floor(afterRaw.length * 0.55);
  const mx = x(mi), my = y(afterRaw[mi]);
  const last = afterPts[afterPts.length - 1];
  const lastBefore = beforePts[beforePts.length - 1];
  const lastVal = afterRaw[afterRaw.length - 1].toFixed(1);
  const lastBeforeVal = beforeRaw[beforeRaw.length - 1].toFixed(1);

  el.innerHTML = `
    <svg viewBox="0 0 ${w} ${h}" width="${w}" height="${h}">
      <defs>
        <linearGradient id="gAB" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="#FF6900" stop-opacity=".14"/>
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
      <text x="${(w - right).toFixed(1)}" y="${(last.y - 8).toFixed(1)}" text-anchor="end" fill="#FF6900" font-family="Inter, sans-serif" font-size="8.5" font-weight="600">${lastVal}</text>
      <text x="${(w - right).toFixed(1)}" y="${(lastBefore.y + 12).toFixed(1)}" text-anchor="end" fill="#A9A9AE" font-family="Inter, sans-serif" font-size="8">${lastBeforeVal}</text>
    </svg>`;
}

/* ---------- mini timeline ---------- */
function drawMini(el, data, color, min, max) {
  if (!el) return;
  const w = el.clientWidth > 40 ? el.clientWidth : 230;
  const h = el.clientHeight || 22;
  const pts = data.map((v, i) => ({
    x: 1 + (i / (data.length - 1)) * (w - 2),
    y: 2 + (1 - (Math.max(min, Math.min(max, v)) - min) / (max - min)) * (h - 4)
  }));
  const d = smoothPath(pts);
  el.innerHTML = `<svg viewBox="0 0 ${w} ${h}" width="${w}" height="${h}"><path d="${d}" fill="none" stroke="${color}" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
}

function drawTimeline() {
  drawMini($('#tlFps'), fpsData.slice(-24), '#FF6900', 96, 124);
  drawMini($('#tlTemp'), tempData, '#FFB27A', 36, 40);
  drawMini($('#tlPower'), powerData, '#0A84FF', 5, 6.2);
  drawMini($('#tlFrame'), fpsData.slice(-24).map(v => 1000 / v), '#8E8E93', 7.5, 12);
  if ($('#tlFpsVal')) $('#tlFpsVal').textContent = fpsData[fpsData.length - 1].toFixed(1);
  if ($('#tlTempVal')) $('#tlTempVal').textContent = tempData[tempData.length - 1].toFixed(1) + '°';
  if ($('#tlPowerVal')) $('#tlPowerVal').textContent = powerData[powerData.length - 1].toFixed(2) + 'W';
  if ($('#tlFrameVal')) $('#tlFrameVal').textContent = (1000 / fpsData[fpsData.length - 1]).toFixed(1) + 'ms';
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
  const low = Math.min(...fpsData.slice(-20)) - 3 + Math.random();
  tweenNum($('#fps'), fps, 1);
  tweenNum($('#low'), Math.max(88, low), 1);
  tweenNum($('#jank'), 1.2 + Math.random() * 1.2, 1);

  const temp = 38.4 + Math.sin(Date.now() / 90000) * 0.7 + Math.random() * 0.3;
  tweenNum($('#temp'), temp, 1);
  $('#tempBar').style.width = Math.min(80, Math.max(20, (temp - 30) * 4)) + '%';
  tempData.push(temp); if (tempData.length > 24) tempData.shift();

  const power = 5.6 + Math.random() * 0.5;
  tweenNum($('#power'), power, 2);
  $('#powerBar').style.width = Math.min(80, power * 10) + '%';
  powerData.push(power); if (powerData.length > 24) powerData.shift();

  const cpu = Math.round(38 + Math.random() * 12);
  const gpu = Math.round(62 + Math.random() * 12);
  $('#cpuVal').textContent = cpu + '%';
  $('#gpuVal').textContent = gpu + '%';
  $$('.load-row .track i')[0].style.width = cpu + '%';
  $$('.load-row .track i')[1].style.width = gpu + '%';

  state.sessionSeconds += 1;
  const mm = String(Math.floor(state.sessionSeconds / 60)).padStart(2, '0');
  const ss = String(state.sessionSeconds % 60).padStart(2, '0');
  $('#session').textContent = `本局 ${mm}:${ss}`;

  if (state.tab === 'monitor') { drawFpsChart(); drawTimeline(); }
}

/* ---------- rendering ---------- */
function updateVerify() {
  $('#verifyChip').textContent = `${state.currentHz}Hz 已核验`;
  $('#refreshValue').textContent = state.refreshLabel;
  $('#refreshSub').textContent = state.refreshMode === 'max'
    ? `当前 ${state.currentHz}Hz`
    : state.refreshMode === 'match' ? '按游戏目标解析' : `已锁定 ${state.currentHz}Hz`;
  $$('#modeChips button').forEach(b => b.classList.toggle('is-on', Number(b.dataset.mode) === state.currentHz));
}

function renderApps() {
  const list = $('#appList');
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
  $('#content').scrollTop = 0;
  try { history.replaceState(null, '', '#' + name); } catch (e) {}
  if (name === 'monitor') requestAnimationFrame(() => { drawFpsChart(); drawTimeline(); });
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

function refreshSheet() {
  const opts = REFRESH_OPTIONS.map(o => {
    const on = o.id === state.refreshMode;
    return `<button class="option ${on ? 'is-on' : ''}" data-refresh="${o.id}">
      <span class="option-radio"></span>
      <span class="option-text"><b>${o.label}</b><em>${o.sub}</em></span>
      <span class="option-tag">${o.tag}</span>
    </button>`;
  }).join('');
  openSheet('刷新率', `<div class="option-list">${opts}</div>
    <p class="sheet-note">选项来自本机真实档位，不存在的档位不会出现。选完后会写入并回读，确认生效才显示「已核验」。</p>`);
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

/* ---------- events ---------- */
document.addEventListener('click', e => {
  const tabBtn = e.target.closest('[data-tab]');
  if (tabBtn) { haptic(); switchTab(tabBtn.dataset.tab); return; }

  const goto = e.target.closest('[data-goto]');
  if (goto) { haptic(); switchTab(goto.dataset.goto); return; }

  const toastBtn = e.target.closest('[data-toast]');
  if (toastBtn) { toast(toastBtn.dataset.toast); return; }

  if (e.target.id === 'scrim' || e.target.closest('#sheetClose')) { closeSheet(); return; }

  const refreshOpt = e.target.closest('[data-refresh]');
  if (refreshOpt) { chooseRefresh(refreshOpt.dataset.refresh); return; }

  const modeChip = e.target.closest('#modeChips button');
  if (modeChip) {
    state.refreshMode = modeChip.dataset.mode;
    state.refreshLabel = `固定 ${modeChip.dataset.mode}Hz`;
    state.currentHz = Number(modeChip.dataset.mode);
    updateVerify(); haptic(); toast(`${state.refreshLabel} · 已核验`);
    return;
  }

  const perfBtn = e.target.closest('#perfSeg button');
  if (perfBtn) {
    $$('#perfSeg button').forEach(b => b.classList.toggle('is-on', b === perfBtn));
    state.perf = perfBtn.textContent;
    haptic();
    toast(`性能档：${state.perf}`);
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

  if (e.target.closest('#recipeSwitch')) {
    state.recipeOn = !state.recipeOn;
    const sw = $('#recipeSwitch');
    sw.classList.toggle('is-on', state.recipeOn);
    toast(state.recipeOn ? '配方已启用' : '配方已暂停'); haptic(); return;
  }

  if (e.target.closest('#refreshRow')) { haptic(); refreshSheet(); return; }
  if (e.target.closest('#exportBtn')) { toast('已生成对比长图（原型）'); haptic(); return; }

  if (e.target.closest('#rescueRow')) {
    openSheet('救援模式', `
      <p class="sheet-note" style="margin-top:0;font-size:10.5px">将把颖控写过的节点逐项还原为备份值，并停止所有配方。不会影响其他模块。</p>
      <button class="action-btn primary" id="rescueConfirm">确认还原（12 项）</button>
      <button class="action-btn" onclick="document.getElementById('sheetClose').click()">取消</button>`);
    setTimeout(() => {
      const btn = $('#rescueConfirm');
      if (btn) btn.addEventListener('click', () => { closeSheet(); toast('已还原 12 个节点（原型）'); haptic(); });
    }, 0);
    return;
  }

  // generic toggles inside recipe (charge limit)
  const sw = e.target.closest('.switch');
  if (sw && sw.id !== 'recipeSwitch') { sw.classList.toggle('is-on'); haptic(); }
});

$('#appSearch').addEventListener('input', e => {
  state.query = e.target.value;
  renderApps();
});

document.addEventListener('keydown', e => { if (e.key === 'Escape') closeSheet(); });

let resizeTimer;
window.addEventListener('resize', () => {
  clearTimeout(resizeTimer);
  resizeTimer = setTimeout(() => { drawFpsChart(); drawTimeline(); drawABChart(); }, 120);
});

// keep charts crisp when fonts load
if (document.fonts && document.fonts.ready) {
  document.fonts.ready.then(() => { requestAnimationFrame(() => { drawFpsChart(); drawABChart(); }); });
}

/* ---------- init ---------- */
function init() {
  const hash = (location.hash || '').replace('#', '');
  if (['monitor', 'records', 'tune', 'apps', 'settings'].includes(hash)) switchTab(hash);
  updateVerify();
  renderApps();
  renderRange();
  requestAnimationFrame(() => { drawFpsChart(); drawTimeline(); drawABChart(); });
  setInterval(() => {
    if (document.hidden) return;
    tickMonitor();
  }, 1000);
}

init();
