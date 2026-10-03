(function () {
    // Inspect only. Native code validates every URL before navigating or downloading.
    const base = location.href;
    const links = Array.from(document.querySelectorAll('a[href]'));
    const unique = values => Array.from(new Map(values.map(v => [v.url, v])).values());
    const buttonLinks = unique(links.filter(a => a.classList.contains('downloadButton') ||
        /\/download\.php\?/.test(a.href)).map(a => ({ url: a.href, label: a.textContent.trim() })));
    const variants = unique(links.filter(a => /-android-apk-download\/?$/.test(new URL(a.href, base).pathname) &&
        a.href !== base && new URL(a.href, base).pathname.startsWith(location.pathname.replace(/\/$/, '') + '/'))
        .map(a => ({ url: a.href, label: (a.closest('.table-row') || a).textContent.trim() })));
    const challenge = !!document.querySelector('#challenge-running, #challenge-stage, iframe[src*="challenges.cloudflare.com"]') ||
        /^(Just a moment|Checking your browser)/i.test(document.title);
    return JSON.stringify({ challenge, next: buttonLinks.length === 1 ? buttonLinks[0].url :
        variants.length === 1 ? variants[0].url : null, variants, title: document.title });
})();
