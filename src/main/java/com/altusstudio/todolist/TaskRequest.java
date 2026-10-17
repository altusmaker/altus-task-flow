package com.altusstudio.todolist;

import java.time.LocalDateTime;

public class TaskRequest {
    private String title;
    private TaskStatus status;
    private LocalDateTime reminderTime;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public LocalDateTime getReminderTime() { return reminderTime; }
    public void setReminderTime(LocalDateTime reminderTime) { this.reminderTime = reminderTime; }
}
