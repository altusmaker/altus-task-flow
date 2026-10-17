(() => {
  'use strict';
  const OFFSET = 210;
  window.reminderInstant = value => { const [date,time]=value.split('T'),[y,m,d]=date.split('-').map(Number),[h,min]=time.split(':').map(Number); return Date.UTC(y,m-1,d,h,min)-OFFSET*60000; };
  function check(){if(Notification.permission!=='granted')return;const now=Date.now();document.querySelectorAll('.task-card[data-tehran-reminder]').forEach(card=>{const remaining=reminderInstant(card.dataset.tehranReminder)-now;if(remaining<=0&&remaining>-60000&&!card.dataset.notified){new Notification('یادآوری تسک',{body:card.querySelector('strong').innerText});card.dataset.notified='true'}})}
  document.addEventListener('DOMContentLoaded',()=>{const valueInput=document.getElementById('newTaskReminder'),displayInput=document.getElementById('newTaskReminderDisplay');window.taskReminderPicker=JalaliDatePicker.create({input:displayInput,valueInput,withTime:true});document.querySelectorAll('.task-card[data-reminder]').forEach(card=>{const value=card.dataset.reminder;card.querySelector('.reminder-badge span').textContent=value.slice(11,16)+' - '+JalaliDatePicker.formatGregorian(value);card.dataset.tehranReminder=value;card.removeAttribute('data-reminder')});const oldOpen=window.openAddModal;window.openAddModal=status=>{oldOpen(status);window.taskReminderPicker.clear()};check();setInterval(check,30000)});
})();
