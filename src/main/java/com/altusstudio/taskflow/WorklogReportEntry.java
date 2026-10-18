package com.altusstudio.taskflow;

import java.time.LocalDateTime;

public record WorklogReportEntry(String taskTitle, int durationMinutes, String description, LocalDateTime loggedAt) {
}
