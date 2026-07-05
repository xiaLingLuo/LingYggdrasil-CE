(async function() {
    try {
        const res = await fetch('/admin/api/dashboard/stats');
        if (res.status === 401) { window.location.href = '/admin/login'; return; }
        const data = await res.json();

        animateNumber('userCount', data.userCount);
        animateNumber('profileCount', data.profileCount);
        animateNumber('activeTokenCount', data.activeTokenCount);
        animateNumber('adminCount', data.adminCount);
    } catch (err) {
        console.error('加载统计数据失败:', err);
    }
})();

function animateNumber(elementId, target) {
    const el = document.getElementById(elementId);
    if (!el) return;
    const duration = 800;
    const start = 0;
    const startTime = performance.now();

    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3);
        const current = Math.round(start + (target - start) * eased);
        el.textContent = current.toLocaleString();
        if (progress < 1) {
            requestAnimationFrame(update);
        }
    }
    requestAnimationFrame(update);
}
