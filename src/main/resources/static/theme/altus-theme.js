(() => {
    const storageKey = 'altus-theme';

    window.AltusTheme = {
        apply(theme) {
            document.documentElement.dataset.theme = theme === 'dark' ? 'dark' : 'light';
            localStorage.setItem(storageKey, document.documentElement.dataset.theme);
        },
        toggle() {
            this.apply(document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark');
        },
        hideLoader(delay = 2200) {
            const loader = document.querySelector('.altus-loader');
            if (!loader) return;
            window.setTimeout(() => loader.classList.add('is-hidden'), delay);
            window.setTimeout(() => loader.remove(), delay + 500);
        }
    };

    AltusTheme.apply(localStorage.getItem(storageKey) || 'light');
    window.addEventListener('load', () => AltusTheme.hideLoader());
})();
