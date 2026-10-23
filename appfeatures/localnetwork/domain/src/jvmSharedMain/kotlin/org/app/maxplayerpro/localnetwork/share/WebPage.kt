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
const tokens = JSON.parse(sessionStorage.getItem('tokens') || '{}');
const unlocked = {};
const ${'$'} = (id) => document.getElementById(id);
const esc = (s) => String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const size = (b) => b > 1e9 ? (b/1e9).toFixed(2)+' GB' : (b/1e6).toFixed(1)+' MB';
const dur = (ms) => { if (!ms) return ''; const s = Math.round(ms/1000), h = Math.floor(s/3600), m = Math.floor(s%3600/60), x = s%60;
  return (h ? h+':'+String(m).padStart(2,'0') : m) + ':' + String(x).padStart(2,'0'); };
const until = (ms) => ms ? 'Until ' + new Date(ms).toLocaleString() : 'No expiry';

async function load() {
  try {
    const info = await (await fetch('/api/v1/info')).json();
    ${'$'}('title').textContent = info.name;
    const shares = await (await fetch('/api/v1/shares')).json();
    ${'$'}('sub').textContent = shares.length + (shares.length === 1 ? ' share' : ' shares') + ' on this device';
    render(shares);
  } catch (e) { ${'$'}('sub').textContent = 'Device not reachable'; }
}

function render(shares) {
  const list = ${'$'}('list');
  if (!shares.length) { list.innerHTML = '<div class="empty">Nothing is shared right now.</div>'; return; }
  list.innerHTML = '';
  for (const s of shares) {
    const open = unlocked[s.id] || (!s.locked ? s : null);
    const el = document.createElement('div');
    el.className = 'share';
    el.innerHTML = '<div class="head"><b>' + esc(s.title) + '</b><span class="meta">' + (s.locked ? '🔒 ' : '') +
      s.itemCount + (s.itemCount === 1 ? ' file' : ' files') + ' · ' + until(s.expiresAtMs) + '</span></div>';
    if (open) {
      for (const it of open.items) {
        const row = document.createElement('div');
        row.className = 'item';
        row.innerHTML = '<span>' + esc(it.name) + '</span><span class="meta">' + [dur(it.durationMs), size(it.sizeBytes)].filter(Boolean).join(' · ') + '</span>';
        row.onclick = () => play(it, tokens[s.id]);
        el.appendChild(row);
      }
    } else {
      const form = document.createElement('form');
      form.innerHTML = '<input inputmode="numeric" autocomplete="one-time-code" placeholder="Enter code"><button>Unlock</button>';
      const err = document.createElement('div');
      err.className = 'err';
      form.onsubmit = async (ev) => {
        ev.preventDefault();
        const r = await fetch('/api/v1/shares/' + s.id + '/unlock', { method:'POST', headers:{'Content-Type':'application/json'},
          body: JSON.stringify({ code: form.querySelector('input').value }) });
        if (r.ok) { const u = await r.json(); tokens[s.id] = u.token; unlocked[s.id] = u.share;
          sessionStorage.setItem('tokens', JSON.stringify(tokens)); render(shares); }
        else err.textContent = r.status === 429 ? 'Too many wrong codes. Wait a minute.' : r.status === 410 ? 'This share has expired.' : 'Wrong code.';
      };
      el.appendChild(form); el.appendChild(err);
    }
    list.appendChild(el);
  }
}

function play(item, token) {
  const url = item.path + (token ? '?t=' + encodeURIComponent(token) : '');
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
