const persianDigits = value => String(value).replace(/\d/g, d => '۰۱۲۳۴۵۶۷۸۹'[d]);
const fromPicker = JalaliDatePicker.create({
    input: document.getElementById('fromPicker'),
    valueInput: document.getElementById('fromIso')
}), toPicker = JalaliDatePicker.create({
    input: document.getElementById('toPicker'),
    valueInput: document.getElementById('toIso')
});
document.getElementById('activeRange').textContent = 'از ' + JalaliDatePicker.formatGregorian(fromIso.value) + ' تا ' + JalaliDatePicker.formatGregorian(toIso.value);
document.querySelectorAll('.persian-number').forEach(element => element.textContent = persianDigits(element.textContent));
document.querySelectorAll('.jalali-date').forEach(element => {
    const value = element.dataset.date.replace('T', ' ');
    element.textContent = JalaliDatePicker.formatGregorian(value) + '، ساعت ' + persianDigits(value.slice(11, 16))
});
document.getElementById('rangeForm').addEventListener('submit', event => {
    event.preventDefault();
    if (!fromIso.value || !toIso.value || fromIso.value > toIso.value) {
        rangeError.classList.remove('d-none');
        return
    }
    location.href = '/worklog-report?from=' + fromIso.value + '&to=' + toIso.value
});
