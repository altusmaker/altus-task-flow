(() => {
    'use strict';
    const persianDigits = value => String(value).replace(/\d/g, digit => '۰۱۲۳۴۵۶۷۸۹'[digit]);

    document.addEventListener('DOMContentLoaded', () => {
        const valueInput = document.getElementById('newTaskReminder'),
            displayInput = document.getElementById('newTaskReminderDisplay');
        window.taskReminderPicker = JalaliDatePicker.create({input: displayInput, valueInput, withTime: true});
        document.querySelectorAll('.task-card[data-reminder]').forEach(card => {
            const value = card.dataset.reminder;
            card.querySelector('.reminder-badge span').textContent = persianDigits(value.slice(11, 16)) + ' - ' + JalaliDatePicker.formatGregorian(value);
            card.dataset.tehranReminder = value;
            card.removeAttribute('data-reminder')
        });
        const oldOpen = window.openAddModal;
        window.openAddModal = status => {
            oldOpen(status);
            window.taskReminderPicker.clear()
        };
    });
})();
