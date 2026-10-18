(() => {
    'use strict';
    const MONTHS = ['فروردین', 'اردیبهشت', 'خرداد', 'تیر', 'مرداد', 'شهریور', 'مهر', 'آبان', 'آذر', 'دی', 'بهمن', 'اسفند'],
        WEEKDAYS = ['ش', 'ی', 'د', 'س', 'چ', 'پ', 'ج'];
    const div = (a, b) => Math.trunc(a / b), mod = (a, b) => a - div(a, b) * b, pad = n => String(n).padStart(2, '0'),
        digits = v => String(v).replace(/\d/g, d => '۰۱۲۳۴۵۶۷۸۹'[d]);

    function jalCal(year) {
        const b = [-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178];
        let l = -14, p = b[0], j;
        for (let i = 1; i < b.length; i++) {
            const n = b[i];
            j = n - p;
            if (year < n) break;
            l += div(j, 33) * 8 + div(mod(j, 33), 4);
            p = n
        }
        let n = year - p;
        l += div(n, 33) * 8 + div(mod(n, 33) + 3, 4);
        if (mod(j, 33) === 4 && j - n === 4) l++;
        const gy = year + 621, lg = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150, march = 20 + l - lg;
        if (j - n < 6) n = n - j + div(j + 4, 33) * 33;
        let leap = mod(mod(n + 1, 33) - 1, 4);
        if (leap === -1) leap = 4;
        return {leap, march}
    }

    function j2g(y, m, d) {
        const r = jalCal(y),
            date = new Date(Date.UTC(y + 621, 2, r.march + (m - 1) * 31 - div(m, 7) * (m - 7) + d - 1));
        return {year: date.getUTCFullYear(), month: date.getUTCMonth() + 1, day: date.getUTCDate()}
    }

    function g2j(iso) {
        const p = {};
        new Intl.DateTimeFormat('en-US-u-ca-persian', {
            timeZone: 'UTC',
            year: 'numeric',
            month: 'numeric',
            day: 'numeric'
        }).formatToParts(new Date(iso.slice(0, 10) + 'T12:00:00Z')).forEach(x => p[x.type] = x.value);
        return {year: +p.year, month: +p.month, day: +p.day}
    }

    function today() {
        const p = {};
        new Intl.DateTimeFormat('en-US-u-ca-persian', {
            timeZone: 'Asia/Tehran',
            year: 'numeric',
            month: 'numeric',
            day: 'numeric'
        }).formatToParts(new Date()).forEach(x => p[x.type] = x.value);
        return {year: +p.year, month: +p.month, day: +p.day}
    }

    const length = (y, m) => m < 7 ? 31 : m < 12 ? 30 : jalCal(y).leap === 0 ? 30 : 29,
        format = d => digits(d.year) + '/' + digits(pad(d.month)) + '/' + digits(pad(d.day));

    function create({input, valueInput, withTime = false, defaultTime = '09:00', onChange}) {
        let selected = valueInput.value ? g2j(valueInput.value) : null, view = selected ? {...selected} : today();
        const pop = document.createElement('div');
        pop.className = 'jalali-picker-popover';
        pop.hidden = true;
        pop.innerHTML = '<div class="jalali-picker" role="dialog" aria-label="انتخاب تاریخ"><div class="jalali-picker-head"><button type="button" class="jalali-prev" aria-label="ماه قبل">‹</button><strong class="jalali-picker-title"></strong><button type="button" class="jalali-next" aria-label="ماه بعد">›</button></div><div class="jalali-week">' + WEEKDAYS.map(d => '<span>' + d + '</span>').join('') + '</div><div class="jalali-days"></div>' + (withTime ? '<div class="jalali-time"><label>ساعت <input type="time" class="form-control jalali-time-input" value="' + defaultTime + '"></label><button type="button" class="btn btn-sm btn-outline-secondary jalali-clear">حذف</button></div>' : '') + '</div>';
        input.insertAdjacentElement('afterend', pop);
        const days = pop.querySelector('.jalali-days'), title = pop.querySelector('.jalali-picker-title'),
            time = pop.querySelector('.jalali-time-input');

        function sync() {
            input.value = selected ? format(selected) + (withTime ? '، ' + time.value : '') : '';
            valueInput.value = selected ? (() => {
                const d = j2g(selected.year, selected.month, selected.day);
                return d.year + '-' + pad(d.month) + '-' + pad(d.day) + (withTime ? 'T' + time.value : '')
            })() : '';
            onChange?.(valueInput.value)
        }

        function render() {
            title.textContent = MONTHS[view.month - 1] + ' ' + digits(view.year);
            const first = j2g(view.year, view.month, 1),
                offset = (new Date(Date.UTC(first.year, first.month - 1, first.day)).getUTCDay() + 1) % 7;
            days.replaceChildren(...Array.from({length: offset}, () => Object.assign(document.createElement('span'), {className: 'empty'})));
            for (let day = 1; day <= length(view.year, view.month); day++) {
                const b = Object.assign(document.createElement('button'), {type: 'button', textContent: digits(day)});
                if (selected && selected.year === view.year && selected.month === view.month && selected.day === day) b.classList.add('selected');
                b.onclick = () => {
                    selected = {...view, day};
                    sync();
                    render();
                    close()
                };
                days.append(b)
            }
        }

        function open() {
            view = selected ? {...selected} : today();
            render();
            pop.hidden = false;
            input.setAttribute('aria-expanded', 'true')
        }

        function close() {
            pop.hidden = true;
            input.setAttribute('aria-expanded', 'false')
        }

        input.readOnly = true;
        input.setAttribute('aria-haspopup', 'dialog');
        input.setAttribute('aria-expanded', 'false');
        input.onclick = () => pop.hidden ? open() : close();
        input.onkeydown = e => {
            if (e.key === 'Escape') close();
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                open()
            }
        };
        pop.querySelector('.jalali-prev').onclick = () => {
            if (--view.month === 0) {
                view.month = 12;
                view.year--
            }
            render()
        };
        pop.querySelector('.jalali-next').onclick = () => {
            if (++view.month === 13) {
                view.month = 1;
                view.year++
            }
            render()
        };
        time?.addEventListener('change', sync);
        pop.querySelector('.jalali-clear')?.addEventListener('click', () => {
            selected = null;
            sync();
            render();
            close()
        });
        document.addEventListener('click', e => {
            if (!pop.hidden && !pop.contains(e.target) && e.target !== input) close()
        });
        sync();
        return {
            clear() {
                selected = null;
                sync();
                render()
            }, setValue(v) {
                valueInput.value = v;
                selected = v ? g2j(v) : null;
                if (v && withTime) time.value = v.slice(11, 16) || defaultTime;
                sync();
                render()
            }, open, close
        }
    }

    window.JalaliDatePicker = {create, formatGregorian: iso => format(g2j(iso))};
})();
