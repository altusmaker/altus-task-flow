package com.altusstudio.todolist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Keeps a human-readable copy of the persisted H2 data in the project data directory.
 */
@Service
public class TodoStorageService {
    private static final Path BACKUP_FILE = Path.of("data", "todo-backup.json");
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    public TodoStorageService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public Task save(Task task) {
        Task savedTask = taskRepository.saveAndFlush(task);
        writeBackup();
        return savedTask;
    }

    public void delete(Long id) {
        taskRepository.deleteById(id);
        taskRepository.flush();
        writeBackup();
    }

    @EventListener(ApplicationReadyEvent.class)
    void createStartupBackup() {
        writeBackup();
    }

    private void writeBackup() {
        try {
            Files.createDirectories(BACKUP_FILE.getParent());
            Path temporaryFile = Files.createTempFile(BACKUP_FILE.getParent(), "todo-backup-", ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporaryFile.toFile(), taskRepository.findAll());
            Files.move(temporaryFile, BACKUP_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write todo backup to " + BACKUP_FILE.toAbsolutePath(), exception);
        }
    }
}
