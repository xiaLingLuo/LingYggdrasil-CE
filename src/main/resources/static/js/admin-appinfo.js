(async function loadAppInfo() {
    try {
        const res = await fetch('/admin/api/appinfo');
        if (res.status === 401) { window.location.href = '/admin/login'; return; }
        const data = await res.json();

        setText('appName', data.appName);
        setText('appVersion', data.appVersion);
        setLink('appRepo', data.repoUrl);
        setText('installedAt', formatDate(data.installedAt));
    } catch (err) {
        console.error('加载应用信息失败:', err);
    }
})();

function setText(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = text || '-';
}

function setLink(id, url) {
    const el = document.getElementById(id);
    if (!el) return;
    if (!url) { el.textContent = '-'; return; }
    const a = document.createElement('a');
    a.href = url;
    a.target = '_blank';
    a.rel = 'noopener';
    a.textContent = url;
    el.replaceChildren(a);
}

function formatDate(dateStr) {
    if (!dateStr || dateStr === '未知') return dateStr || '-';
    try {
        const d = new Date(dateStr);
        return d.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }) +
            ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    } catch { return dateStr; }
}
