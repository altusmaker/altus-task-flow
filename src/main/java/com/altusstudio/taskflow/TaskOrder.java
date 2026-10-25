package com.altusstudio.taskflow;

import java.util.Comparator;

/** Shared ordering for the board and desktop task list. */
final class TaskOrder {
    private TaskOrder() {}

    static final Comparator<Task> BOARD = Comparator.comparing(Task::getReminderTime,
                    Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Task::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Task::getId, Comparator.reverseOrder());
}
