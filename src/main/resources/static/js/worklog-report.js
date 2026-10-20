const persianDigits = value => String(value).replace(/\d/g, d => '۰۱۲۳۴۵۶۷۸۹'[d]);
const applyStoredTheme = () => {
    const savedTheme = localStorage.getItem('todo-theme') || localStorage.getItem('altus-theme');
    document.documentElement.dataset.theme = savedTheme === 'dark' ? 'dark' : 'light';
};

applyStoredTheme();
window.addEventListener('storage', event => {
    if (event.key === 'todo-theme' || event.key === 'altus-theme') applyStoredTheme();
});

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

const editWorklogModal = new bootstrap.Modal(document.getElementById('editWorklogModal'));
const editWorklogTaskId = document.getElementById('editWorklogTaskId');
const editWorklogId = document.getElementById('editWorklogId');
const editWorklogDuration = document.getElementById('editWorklogDuration');
const editWorklogDescription = document.getElementById('editWorklogDescription');

function formatWorklogDuration(minutes) {
    const hours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60;
    return hours ? `${hours}h ${remainingMinutes}m` : `${remainingMinutes}m`;
}

function parseWorklogDuration(value) {
    const match = value.trim().match(/^(?:(\d+)h)?\s*(?:(\d+)m)?$/i);
    if (!match || (!match[1] && !match[2])) return null;
    const minutes = Number(match[1] || 0) * 60 + Number(match[2] || 0);
    return minutes > 0 ? minutes : null;
}

document.querySelectorAll('.edit-worklog').forEach(button => button.addEventListener('click', () => {
    editWorklogTaskId.value = button.dataset.taskId;
    editWorklogId.value = button.dataset.worklogId;
    editWorklogDuration.value = formatWorklogDuration(Number(button.dataset.minutes));
    editWorklogDescription.value = button.dataset.description || '';
    editWorklogDuration.classList.remove('is-invalid');
    editWorklogModal.show();
}));

document.getElementById('saveWorklogEdit').addEventListener('click', async () => {
    const minutes = parseWorklogDuration(editWorklogDuration.value);
    if (minutes === null) {
        editWorklogDuration.classList.add('is-invalid');
        editWorklogDuration.focus();
        return;
    }
    editWorklogDuration.classList.remove('is-invalid');
    const response = await fetch(`/api/tasks/${editWorklogTaskId.value}/worklog/${editWorklogId.value}`, {
        method: 'PUT', headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({minutes, description: editWorklogDescription.value})
    });
    if (!response.ok) {
        alert('ذخیره تغییرات انجام نشد. لطفاً دوباره تلاش کنید.');
        return;
    }
    location.reload();
});
