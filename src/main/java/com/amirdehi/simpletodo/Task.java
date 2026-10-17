package com.amirdehi.simpletodo;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String title;
    @Enumerated(EnumType.STRING) private TaskStatus status;
    private LocalDateTime reminderTime;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER) private List<WorkLog> workLogs = new ArrayList<>();

    public Task() { }
    public Task(Long id, String title, TaskStatus status, LocalDateTime reminderTime, List<WorkLog> workLogs) { this.id = id; this.title = title; this.status = status; this.reminderTime = reminderTime; this.workLogs = workLogs; }
    public long getTotalWorkMinutes() { return workLogs.stream().mapToLong(WorkLog::getDurationMinutes).sum(); }
    public String getFormattedTotalWork() {
        long totalMinutes = getTotalWorkMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public LocalDateTime getReminderTime() { return reminderTime; }
    public void setReminderTime(LocalDateTime reminderTime) { this.reminderTime = reminderTime; }
    public List<WorkLog> getWorkLogs() { return workLogs; }
    public void setWorkLogs(List<WorkLog> workLogs) { this.workLogs = workLogs; }
}
