package org.app.maxplayerpro.localnetwork.share

/** The page a browser gets at the host's address: lists shares, unlocks coded ones and plays items in place. */
internal val WEB_PAGE = """
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Max Player Share</title>
<style>
  :root { --bg:#12161c; --card:#1c222b; --line:#2a313c; --text:#e8ecf1; --muted:#9aa4b1; --accent:#3d8bfd; --bad:#ff6b6b; }
  @media (prefers-color-scheme: light) { :root { --bg:#f5f6f8; --card:#fff; --line:#e1e4e8; --text:#1b1f24; --muted:#5d6672; } }
  * { box-sizing:border-box; }
  body { margin:0; background:var(--bg); color:var(--text); font:15px/1.45 system-ui, -apple-system, Segoe UI, Roboto, sans-serif; }
  header { padding:16px; border-bottom:1px solid var(--line); }
  header h1 { margin:0; font-size:18px; }
  header p { margin:2px 0 0; color:var(--muted); font-size:13px; }
  main { max-width:960px; margin:0 auto; padding:16px; }
  video, audio { width:100%; max-height:70vh; background:#000; border-radius:10px; display:none; margin-bottom:8px; }
  #now { color:var(--muted); font-size:13px; margin-bottom:16px; min-height:1em; word-break:break-all; }
  .share { background:var(--card); border:1px solid var(--line); border-radius:12px; margin-bottom:12px; overflow:hidden; }
  .head { padding:12px 14px; display:flex; gap:8px; align-items:center; justify-content:space-between; flex-wrap:wrap; }
  .head b { font-size:15px; }
  .meta { color:var(--muted); font-size:12px; }
  .item { display:flex; justify-content:space-between; gap:12px; padding:10px 14px; border-top:1px solid var(--line); cursor:pointer; }
  .item:hover { background:rgba(127,127,127,.08); }
  .item span:first-child { word-break:break-all; }
  form { display:flex; gap:8px; padding:0 14px 12px; }
  input { flex:1; min-width:0; padding:8px 10px; border-radius:8px; border:1px solid var(--line); background:var(--bg); color:var(--text); font-size:16px; letter-spacing:2px; }
  form { padding-top:0; }
  button { padding:8px 14px; border:0; border-radius:8px; background:var(--accent); color:#fff; font-weight:600; cursor:pointer; }
  .err { color:var(--bad); font-size:13px; padding:0 14px 10px; }
  .empty { color:var(--muted); text-align:center; padding:40px 0; }
</style>
</head>
<body>
<header><h1 id="title">Max Player Share</h1><p id="sub">Loading…</p></header>
<main>
  <video id="video" controls playsinline></video>
  <audio id="audio" controls></audio>
  <div id="now"></div>
  <div id="list"></div>
</main>
<script>
// PINs are remembered as the tokens they return, per device, until the owner logs everyone out.
const keyOf = (id) => 'mp-share-' + id;
let device = null, access = { d: null, t: {} };
const unlocked = {};
const ${'$'} = (id) => document.getElementById(id);
const esc = (s) => String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const size = (b) => b > 1e9 ? (b/1e9).toFixed(2)+' GB' : (b/1e6).toFixed(1)+' MB';
const dur = (ms) => { if (!ms) return ''; const s = Math.round(ms/1000), h = Math.floor(s/3600), m = Math.floor(s%3600/60), x = s%60;
  return (h ? h+':'+String(m).padStart(2,'0') : m) + ':' + String(x).padStart(2,'0'); };
const until = (ms) => ms ? 'Until ' + new Date(ms).toLocaleString() : 'No time limit';
const save = () => { try { localStorage.setItem(keyOf(device.id), JSON.stringify(access)); } catch (e) {} };
const headers = (t) => { const h = {}; if (access.d) h['X-Device-Token'] = access.d; if (t) h['X-Share-Token'] = t; return h; };

async function load() {
  try {
    device = await (await fetch('/api/v1/info')).json();
    ${'$'}('title').textContent = device.name;
    try { access = JSON.parse(localStorage.getItem(keyOf(device.id))) || access; } catch (e) {}
    if (device.pinRequired && !access.d) return askDevicePin();
    const r = await fetch('/api/v1/shares', { headers: headers() });
    if (r.status === 401) { access = { d: null, t: {} }; save(); return askDevicePin(); }
    if (r.status === 410) { ${'$'}('sub').textContent = 'Sharing time is over'; return; }
    const shares = await r.json();
    for (const s of shares.filter(s => s.locked && access.t[s.id])) {
      const one = await fetch('/api/v1/shares/' + s.id, { headers: headers(access.t[s.id]) });
      if (one.ok) unlocked[s.id] = await one.json(); else { delete access.t[s.id]; save(); }
    }
    ${'$'}('sub').textContent = shares.length + (shares.length === 1 ? ' share' : ' shares') + ' on this device';
    render(shares);
  } catch (e) { ${'$'}('sub').textContent = 'Device not reachable'; }
}

function pinForm(label, onPin) {
  const wrap = document.createElement('div');
  const form = document.createElement('form');
  form.innerHTML = '<input type="password" inputmode="numeric" autocomplete="off" placeholder="' + label + '"><button>Open</button>';
  const err = document.createElement('div'); err.className = 'err';
  form.onsubmit = async (ev) => { ev.preventDefault(); err.textContent = await onPin(form.querySelector('input').value) || ''; };
  wrap.appendChild(form); wrap.appendChild(err);
  return wrap;
}

const failText = (status) => status === 429 ? 'Too many wrong PINs. Wait a minute.' : status === 410 ? 'Sharing time is over.' : 'Wrong PIN.';

function askDevicePin() {
  ${'$'}('sub').textContent = 'This device has a PIN';
  const list = ${'$'}('list'); list.innerHTML = '';
  const box = document.createElement('div'); box.className = 'share';
  box.innerHTML = '<div class="head"><b>Enter the device PIN</b><span class="meta">Asked once on this browser</span></div>';
  box.appendChild(pinForm('Device PIN', async (pin) => {
    const r = await fetch('/api/v1/login', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({ pin }) });
    if (!r.ok) return failText(r.status);
    access = { d: (await r.json()).token, t: {} }; save(); load();
  }));
  list.appendChild(box);
}

function render(shares) {
  const list = ${'$'}('list');
  if (!shares.length) { list.innerHTML = '<div class="empty">Nothing is shared right now.</div>'; return; }
  list.innerHTML = '';
  shares.sort((a, b) => (b.folder - a.folder) || a.title.localeCompare(b.title));
  for (const s of shares) {
    const open = unlocked[s.id] || (!s.locked ? s : null);
    const el = document.createElement('div');
    el.className = 'share';
    el.innerHTML = '<div class="head"><b>' + (s.folder ? '📁 ' : '') + esc(s.title) + '</b><span class="meta">' + (s.locked ? '🔒 Protected · ' : '') +
      s.itemCount + (s.itemCount === 1 ? ' file' : ' files') + ' · ' + until(s.expiresAtMs) + '</span></div>';
    if (open) {
      for (const it of open.items) {
        const row = document.createElement('div');
        row.className = 'item';
        row.innerHTML = '<span>' + esc(it.name) + '</span><span class="meta">' + [dur(it.durationMs), size(it.sizeBytes)].filter(Boolean).join(' · ') + '</span>';
        row.onclick = () => play(it, access.t[s.id]);
        el.appendChild(row);
      }
    } else {
      el.appendChild(pinForm('PIN for this share', async (pin) => {
        const r = await fetch('/api/v1/shares/' + s.id + '/unlock', { method:'POST', headers: Object.assign({'Content-Type':'application/json'}, headers()),
          body: JSON.stringify({ code: pin }) });
        if (!r.ok) return failText(r.status);
        const u = await r.json(); access.t[s.id] = u.token; unlocked[s.id] = u.share; save(); render(shares);
      }));
    }
    list.appendChild(el);
  }
}

function play(item, token) {
  const q = [access.d && 'd=' + encodeURIComponent(access.d), token && 't=' + encodeURIComponent(token)].filter(Boolean).join('&');
  const url = item.path + (q ? '?' + q : '');
  const v = ${'$'}('video'), a = ${'$'}('audio');
  const el = item.isAudio ? a : v, other = item.isAudio ? v : a;
  other.pause(); other.style.display = 'none';
  el.src = url; el.style.display = 'block'; el.play().catch(() => {});
  ${'$'}('now').textContent = item.name;
  el.onerror = () => { ${'$'}('now').textContent = item.name + ' — this browser cannot play this format. Open it in Max Player or VLC: ' + location.origin + url; };
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

load();
</script>
</body>
</html>
"""
