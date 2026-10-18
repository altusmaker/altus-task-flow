package com.altusstudio.taskflow;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class WorkLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int durationMinutes;
    private String description;
    private LocalDateTime loggedAt;

    public WorkLog() {
    }

    public WorkLog(Long id, int durationMinutes, String description, LocalDateTime loggedAt) {
        this.id = id;
        this.durationMinutes = durationMinutes;
        this.description = description;
        this.loggedAt = loggedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }
}
