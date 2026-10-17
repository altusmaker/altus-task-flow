package com.altusstudio.todolist;

public class TaskRequest {
    private String title;
    private String label;
    private TaskStatus status;
    private String reminderTime;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getReminderTime() { return reminderTime; }
    public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }
}
