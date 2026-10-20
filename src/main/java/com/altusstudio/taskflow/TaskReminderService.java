package com.altusstudio.taskflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TaskReminderService {
    private final TaskRepository tasks;
    private final DesktopNotificationService notifications;
    private final Clock clock;
    private final Map<Long, LocalDateTime> delivered = new HashMap<>();

    @Autowired
    public TaskReminderService(TaskRepository tasks, DesktopNotificationService notifications) {
        this(tasks, notifications, Clock.system(ZoneId.of("Asia/Tehran")));
    }

    TaskReminderService(TaskRepository tasks, DesktopNotificationService notifications, Clock clock) {
        this.tasks = tasks;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 15000)
    public void checkReminders() {
        if (!notifications.isAvailable()) return;
        LocalDateTime now = LocalDateTime.now(clock);
        var currentTasks = tasks.findAll();
        var reminders = currentTasks.stream()
                .filter(task -> task.getReminderTime() != null)
                .collect(Collectors.toMap(Task::getId, Task::getReminderTime));
        delivered.entrySet().removeIf(entry -> !entry.getValue().equals(reminders.get(entry.getKey())));
        for (Task task : currentTasks) {
            LocalDateTime reminder = task.getReminderTime();
            // Preserve the existing one-minute due window; do not replay old reminders on startup.
            if (reminder != null && !reminder.isAfter(now) && reminder.isAfter(now.minusMinutes(1))
                    && !reminder.equals(delivered.get(task.getId()))
                    && notifications.showReminder(task.getTitle())) {
                delivered.put(task.getId(), reminder);
            }
        }
    }
}
