// Run in the head before the loader can paint, including after task mutations.
(() => {
    const navigation = performance.getEntriesByType('navigation')[0];
    const referrer = document.referrer ? new URL(document.referrer) : null;
    const samePageNavigation = navigation?.type === 'navigate'
        && referrer?.origin === location.origin
        && referrer.pathname === location.pathname;
    const suppressLoader = navigation?.type === 'reload' || samePageNavigation;

    if (suppressLoader) document.documentElement.setAttribute('data-suppress-page-loader', '');

    window.addEventListener('load', () => {
        const loader = document.querySelector('.app-loader');
        if (!loader) return;
        if (suppressLoader) {
            loader.remove();
            return;
        }
        window.setTimeout(() => loader.classList.add('is-hidden'), 1500);
        window.setTimeout(() => loader.remove(), 1700);
    });
})();
