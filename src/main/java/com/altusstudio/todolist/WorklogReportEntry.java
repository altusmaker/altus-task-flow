package com.altusstudio.todolist;

import java.time.LocalDateTime;

/**
 * A single work-log row together with the task it belongs to.
 */
public record WorklogReportEntry(String taskTitle, int durationMinutes, String description, LocalDateTime loggedAt) {
}
