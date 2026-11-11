(function () {
    function addHelp(target, text) {
        if (!target || !text) return;
        if (target.querySelector('.ui-help')) return;
        var help = document.createElement('button');
        help.type = 'button';
        help.className = 'ui-help';
        help.tabIndex = 0;
        help.setAttribute('aria-label', '' + t('common.fieldHint') + '');
        help.setAttribute('data-tooltip', text);
        help.textContent = '?';
        target.appendChild(document.createTextNode(' '));
        target.appendChild(help);
    }

    function tooltipify() {
        document.querySelectorAll('.setting-info').forEach(function (info) {
            var desc = info.querySelector('.setting-desc');
            if (!desc) return;
            var text = (desc.textContent || '').trim();
            var label = info.querySelector('.setting-label');
            if (text) addHelp(label, text);
            desc.remove();
        });
        document.querySelectorAll('.setting-desc').forEach(function (desc) {
            var text = (desc.textContent || '').trim();
            var card = desc.closest('.card');
            var title = card ? card.querySelector('.card-title') : null;
            if (text && title) addHelp(title, text);
            desc.remove();
        });
    }

    var scheduled = false;
    function schedule() {
        if (scheduled) return;
        scheduled = true;
        setTimeout(function () {
            scheduled = false;
            tooltipify();
        }, 0);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', schedule);
    } else {
        schedule();
    }
    new MutationObserver(function (mutations) {
        for (var i = 0; i < mutations.length; i++) {
            if (mutations[i].addedNodes && mutations[i].addedNodes.length) {
                schedule();
                return;
            }
        }
    }).observe(document.body, { childList: true, subtree: true });
})();
