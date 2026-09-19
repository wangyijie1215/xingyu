/* EchoFlow 原型路由：屏幕切换、Tab 高亮、浮层、左侧导航 */
(function () {
  const TAB_SCREENS = ['home', 'moments', 'discover', 'me'];
  const screens = Array.from(document.querySelectorAll('.screen'));
  const tabs = Array.from(document.querySelectorAll('#tabbar button'));
  const tabbar = document.getElementById('tabbar');
  const history = [];

  function show(name, push) {
    const target = screens.find(s => s.dataset.screen === name);
    if (!target) return;
    const current = screens.find(s => s.classList.contains('active'));
    if (current && current !== target && push !== false) history.push(current.dataset.screen);
    screens.forEach(s => s.classList.toggle('active', s === target));
    target.scrollTop = 0;

    // Tab 高亮 + 是否显示底部 Tab
    tabs.forEach(b => b.classList.toggle('on', b.dataset.go === name));
    const isTabScreen = TAB_SCREENS.indexOf(name) >= 0;
    tabbar.style.display = isTabScreen ? 'grid' : 'none';
    document.querySelector('.viewport').style.paddingBottom = '0';
  }

  function back(fallback) {
    const prev = history.pop();
    show(prev || fallback || 'home', false);
  }

  // data-go / data-back
  document.addEventListener('click', function (e) {
    const go = e.target.closest('[data-go]');
    if (go) { show(go.dataset.go); return; }
    const bk = e.target.closest('[data-back]');
    if (bk) { back(bk.dataset.back); return; }

    const ov = e.target.closest('[data-overlay]');
    if (ov) { openOverlay(ov.dataset.overlay); return; }
    if (e.target.closest('[data-close]')) { closeOverlays(); return; }

    // 剧情分支按钮
    const br = e.target.closest('.branches button');
    if (br) { br.style.background = 'rgba(155,123,255,.28)'; br.style.borderColor = '#C7A8FF'; }
  });

  function openOverlay(id) {
    const el = document.getElementById('ov-' + id);
    if (el) el.classList.add('on');
  }
  function closeOverlays() {
    document.querySelectorAll('.overlay').forEach(o => o.classList.remove('on'));
  }
  document.addEventListener('keydown', e => { if (e.key === 'Escape') closeOverlays(); });

  // 左侧快速跳转
  const jump = document.getElementById('jump');
  const labels = { home: '陪伴首页', charhome: '角色主页', chat: '聊天', memory: '记忆库', events: '事件簿',
                   moments: '朋友圈', discover: '发现', me: '我的', call: '语音通话', settings: 'Provider',
                   scenario: '剧情/世界观', relation: '关系与情绪',
                   shrine: '神社·每日运势', omikuji: '摇签', fortune: '签文', ema: '绘马架', diary: '她的日记',
                   letters: '信笺', capsule: '时间胶囊' };
  screens.forEach(s => {
    const b = document.createElement('button');
    b.textContent = labels[s.dataset.screen] || s.dataset.screen;
    b.onclick = () => show(s.dataset.screen);
    jump.appendChild(b);
  });

  // ---------- 樱花花瓣：神社相关屏幕的「在场感」 ----------
  function bloomPetals(host, n) {
    if (!host) return;
    host.innerHTML = '';
    for (let i = 0; i < n; i++) {
      const p = document.createElement('i');
      p.style.left = (Math.random() * 100) + '%';
      p.style.animationDuration = (6 + Math.random() * 7).toFixed(1) + 's';
      p.style.animationDelay = (-Math.random() * 10).toFixed(1) + 's';
      p.style.transform = 'scale(' + (0.6 + Math.random() * 0.9).toFixed(2) + ')';
      host.appendChild(p);
    }
  }
  bloomPetals(document.getElementById('petals'), 14);
  bloomPetals(document.getElementById('petals2'), 16);

  // ---------- 摇签流程 ----------
  const tube = document.getElementById('tube');
  const shakeBtn = document.getElementById('shake-btn');
  const dropZone = document.getElementById('drop-zone');
  const prayer = document.getElementById('prayer');
  let shook = false;

  if (shakeBtn && tube) {
    shakeBtn.addEventListener('click', () => {
      if (shook) { show('fortune'); return; }
      shook = true;
      shakeBtn.textContent = '摇 签 中 …';
      shakeBtn.style.opacity = '.7';
      tube.classList.add('shaking');
      if (prayer) prayer.innerHTML = '签 筒 在 手 里 发 出 细 碎 的 声 响 …';

      setTimeout(() => {
        tube.classList.remove('shaking');
        dropZone.innerHTML = '<div class="falling-stick"></div>';
        if (prayer) prayer.innerHTML = '一 支 签 落 了 出 来 。';
      }, 1400);

      setTimeout(() => {
        shakeBtn.textContent = '展 开 签 文';
        shakeBtn.style.opacity = '1';
        shakeBtn.style.background = 'linear-gradient(135deg,#C99A22,#B08D3C)';
      }, 2600);
    });
  }

  show('home', false);
  window.__echoflow = { show: show };
})();
