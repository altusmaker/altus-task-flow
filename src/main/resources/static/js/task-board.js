const addModal = new bootstrap.Modal(document.getElementById('addTaskModal'));
const worklogModal = new bootstrap.Modal(document.getElementById('worklogModal'));

function openAddModal(status) {
    newTaskStatus.value = status;
    newTaskTitle.value = '';
    newTaskLabel.value = '';
    newTaskReminder.value = '';
    addModal.show()
}

function deleteTask(id) {
    if (confirm('آیا از حذف این تسک اطمینان دارید؟')) fetch('/api/tasks/' + id, {method: 'DELETE'}).then(() => location.reload())
}

function openWorklogModal(id) {
    worklogTaskId.value = id;
    worklogDuration.value = '';
    worklogDuration.classList.remove('is-invalid');
    worklogDesc.value = '';
    worklogModal.show()
}

function parseWorklogDuration(value) {
    const match = value.trim().match(/^(?:(\d+)\s*h)?\s*(?:(\d+)\s*m)?$/i);
    if (!match || (!match[1] && !match[2])) return null;
    const minutes = Number(match[1] || 0) * 60 + Number(match[2] || 0);
    return minutes > 0 ? minutes : null
}

function saveWorklog() {
    const minutes = parseWorklogDuration(worklogDuration.value);
    if (minutes === null) {
        worklogDuration.classList.add('is-invalid');
        worklogDuration.focus();
        return
    }
    worklogDuration.classList.remove('is-invalid');
    fetch(`/api/tasks/${worklogTaskId.value}/worklog`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({minutes, description: worklogDesc.value})
    }).then(() => location.reload())
}

function updateTaskCounts() {
    document.querySelectorAll('.column').forEach(column => column.querySelector('.task-count').textContent = new Intl.NumberFormat('fa-IR', {useGrouping: false}).format(column.querySelectorAll('.task-list .task-card').length))
}

function drag(event) {
    event.dataTransfer.setData('text', event.target.id)
}

function dragEnd() {
    document.querySelectorAll('.column').forEach(column => column.classList.remove('drag-over'))
}

function allowDrop(event) {
    event.preventDefault();
    event.currentTarget.classList.add('drag-over')
}

function dragLeave(event) {
    if (!event.currentTarget.contains(event.relatedTarget)) event.currentTarget.classList.remove('drag-over')
}

function drop(event) {
    event.preventDefault();
    const column = event.currentTarget, id = event.dataTransfer.getData('text'), task = document.getElementById(id);
    column.classList.remove('drag-over');
    column.querySelector('.task-list').appendChild(task);
    updateTaskCounts();
    fetch(`/api/tasks/${id.replace('task-', '')}/status?status=${column.dataset.status}`, {method: 'PATCH'})
}

function setupTheme() {
    const storageKey = 'todo-theme', button = document.createElement('button');
    button.type = 'button';
    button.className = 'btn theme-toggle';
    const applyTheme = theme => {
        const dark = theme === 'dark';
        document.documentElement.dataset.theme = dark ? 'dark' : 'light';
        button.setAttribute('aria-pressed', String(dark));
        button.textContent = dark ? '☀️ حالت روشن' : '🌙 حالت تاریک'
    };
    applyTheme(localStorage.getItem(storageKey) === 'dark' ? 'dark' : 'light');
    button.addEventListener('click', () => {
        const theme = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
        localStorage.setItem(storageKey, theme);
        applyTheme(theme)
    });
    document.querySelector('.notify-button').insertAdjacentElement('beforebegin', button)
}

function setupBrand() {
    const header = document.querySelector('.board-header'), titleBlock = header?.firstElementChild;
    if (!titleBlock || titleBlock.querySelector('.brand-logo')) return;
    const brand = document.createElement('div');
    brand.className = 'd-flex align-items-start';
    header.insertBefore(brand, titleBlock);
    brand.appendChild(titleBlock);
    const logo = document.createElement('img');
    logo.className = 'brand-logo';
    logo.src = '/images/todo-logo.png';
    logo.alt = '';
    brand.insertBefore(logo, titleBlock)
}

function setupTaskEditor() {
    const modalTitle = document.getElementById('taskModalTitle'), taskId = document.getElementById('editingTaskId'),
        title = document.getElementById('newTaskTitle'), label = document.getElementById('newTaskLabel'),
        status = document.getElementById('newTaskStatus'), reminder = document.getElementById('newTaskReminder'),
        saveButton = document.getElementById('saveTaskButton'),
        confirmModal = new bootstrap.Modal(document.getElementById('confirmTaskUpdateModal')),
        confirmButton = document.getElementById('confirmTaskUpdateButton');
    let dragged = false;

    async function persist() {
        if (!title.value.trim()) {
            title.classList.add('is-invalid');
            title.focus();
            return
        }
        if (reminder.value && !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2})?$/.test(reminder.value)) {
            alert('تاریخ یادآوری نامعتبر است.');
            return
        }
        title.classList.remove('is-invalid');
        saveButton.disabled = true;
        confirmButton.disabled = true;
        try {
            const response = await fetch(taskId.value ? `/api/tasks/${taskId.value}` : '/api/tasks', {
                method: taskId.value ? 'PUT' : 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    title: title.value.trim(),
                    label: label.value.trim() || null,
                    status: status.value,
                    reminderTime: reminder.value ? `${reminder.value.slice(0, 16)}:00` : null
                })
            });
            if (!response.ok) throw new Error(response.status);
            location.reload()
        } catch (error) {
            saveButton.disabled = false;
            confirmButton.disabled = false;
            alert('امکان ذخیره تسک وجود ندارد.');
            console.error(error)
        }
    }

    window.openAddModal = statusValue => {
        taskId.value = '';
        modalTitle.textContent = 'افزودن تسک';
        saveButton.textContent = 'ذخیره تسک';
        newTaskStatus.value = statusValue;
        title.value = '';
        label.value = '';
        reminder.value = '';
        addModal.show()
    };
    window.openEditTaskModal = card => {
        taskId.value = card.id.replace('task-', '');
        status.value = card.closest('.column').dataset.status;
        title.value = card.dataset.title || '';
        label.value = card.dataset.label || '';
        modalTitle.textContent = 'ویرایش تسک';
        saveButton.textContent = 'ذخیره تغییرات';
        title.classList.remove('is-invalid');
        window.taskReminderPicker.setValue(card.dataset.tehranReminder || card.dataset.reminder || '');
        addModal.show()
    };
    window.saveTask = () => {
        if (!taskId.value) {
            persist();
            return
        }
        if (!title.value.trim()) {
            title.classList.add('is-invalid');
            title.focus();
            return
        }
        confirmModal.show()
    };
    confirmButton.addEventListener('click', () => {
        confirmModal.hide();
        persist()
    });
    document.querySelectorAll('.task-card').forEach(card => {
        card.addEventListener('dragstart', () => dragged = true);
        card.addEventListener('dragend', () => setTimeout(() => dragged = false, 0));
        card.addEventListener('click', event => {
            if (!dragged && !event.target.closest('button,a,input,textarea,select')) window.openEditTaskModal(card)
        })
    })
}

document.addEventListener('DOMContentLoaded', () => {
    updateTaskCounts();
    setupTheme();
    setupBrand();
    setupTaskEditor()
});

window.addEventListener('load', () => {
    const loader = document.querySelector('.app-loader');
    if (!loader) return;
    window.setTimeout(() => loader.classList.add('is-hidden'), 1500);
    window.setTimeout(() => loader.remove(), 1700)
});
