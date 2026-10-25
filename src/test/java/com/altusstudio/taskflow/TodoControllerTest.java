package com.altusstudio.taskflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TodoControllerTest {
    private TaskRepository repository;
    private TodoStorageService storage;
    private TodoController controller;
    private Task task;

    @BeforeEach
    void setUp() {
        repository = mock(TaskRepository.class);
        storage = mock(TodoStorageService.class);
        controller = new TodoController(repository, storage, mock(DesktopNotificationService.class));
        task = new Task();
        task.setStatus(TaskStatus.TODO);
        when(repository.findById(1L)).thenReturn(Optional.of(task));
        when(storage.save(task)).thenReturn(task);
    }

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void submittingWorklogMovesTaskToInProgress(TaskStatus initialStatus) {
        task.setStatus(initialStatus);
        WorkLogRequest request = new WorkLogRequest();
        request.setMinutes(45);
        request.setDescription("Implemented change");

        var response = controller.addWorkLog(1L, request);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertSame(task, response.getBody());
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertEquals(1, task.getWorkLogs().size());
        assertEquals(45, task.getWorkLogs().get(0).getDurationMinutes());
        verify(storage).save(task);
    }

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void tasksWithWorklogsCannotMoveToTodo(TaskStatus initialStatus) {
        task.setStatus(initialStatus);
        // Existence, not total duration, determines whether moving to TODO is allowed.
        task.getWorkLogs().add(new WorkLog(1L, 0, "Existing work", LocalDateTime.now()));

        var response = controller.updateStatus(1L, TaskStatus.TODO);

        assertEquals(400, response.getStatusCode().value());
        assertEquals(initialStatus, task.getStatus());
        verifyNoInteractions(storage);
    }

    @Test
    void tasksWithoutWorklogsCanMoveToTodo() {
        task.setStatus(TaskStatus.IN_PROGRESS);

        assertTrue(controller.updateStatus(1L, TaskStatus.TODO).getStatusCode().is2xxSuccessful());

        assertEquals(TaskStatus.TODO, task.getStatus());
        verify(storage).save(task);
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"IN_PROGRESS", "DONE"})
    void tasksWithWorklogsCanMoveToOtherColumns(TaskStatus targetStatus) {
        task.getWorkLogs().add(new WorkLog(1L, 30, "Existing work", LocalDateTime.now()));

        assertTrue(controller.updateStatus(1L, targetStatus).getStatusCode().is2xxSuccessful());

        assertEquals(targetStatus, task.getStatus());
        verify(storage).save(task);
    }
}
