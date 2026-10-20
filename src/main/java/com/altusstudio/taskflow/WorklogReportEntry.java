package com.altusstudio.taskflow;

import java.time.LocalDateTime;

public record WorklogReportEntry(Long taskId, Long workLogId, String taskTitle, int durationMinutes,
                                 String description, LocalDateTime loggedAt) {
}
