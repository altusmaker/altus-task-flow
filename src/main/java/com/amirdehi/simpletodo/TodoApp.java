package com.amirdehi.simpletodo;

import jakarta.persistence.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
public class TodoApp {
    public static void main(String[] args) {
        SpringApplication.run(TodoApp.class, args);
    }
}

// --- ENUMS ---
enum TaskStatus {
    TODO("آماده انجام"),
    IN_PROGRESS("در حال انجام"),
    DONE("انجام شده");

    public final String label;
    TaskStatus(String label) { this.label = label; }
}

// --- ENTITIES ---
@Entity
class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    private LocalDateTime reminderTime;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<WorkLog> workLogs = new ArrayList<>();

    public long getTotalWorkMinutes() {
        return workLogs.stream().mapToLong(WorkLog::getDurationMinutes).sum();
    }

    public Task() { }
    public Task(Long id, String title, TaskStatus status, LocalDateTime reminderTime, List<WorkLog> workLogs) {
        this.id = id; this.title = title; this.status = status; this.reminderTime = reminderTime; this.workLogs = workLogs;
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

@Entity
class WorkLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int durationMinutes;
    private String description;
    private LocalDateTime loggedAt;

    public WorkLog() { }
    public WorkLog(Long id, int durationMinutes, String description, LocalDateTime loggedAt) {
        this.id = id; this.durationMinutes = durationMinutes; this.description = description; this.loggedAt = loggedAt;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}

// --- REPOSITORIES ---
interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByOrderByStatusAsc();
}

/** Keeps a human-readable copy of the persisted H2 data in the project data directory. */
@Service
class TodoStorageService {
    private static final Path BACKUP_FILE = Path.of("data", "todo-backup.json");

    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    TodoStorageService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    Task save(Task task) {
        Task savedTask = taskRepository.saveAndFlush(task);
        writeBackup();
        return savedTask;
    }

    void delete(Long id) {
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

// --- DTOs ---
class TaskRequest {
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

class WorkLogRequest {
    private int minutes;
    private String description;
    public int getMinutes() { return minutes; }
    public void setMinutes(int minutes) { this.minutes = minutes; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

// --- CONTROLLER ---
@Controller
class TodoController {

    private final TaskRepository taskRepository;
    private final TodoStorageService storageService;

    TodoController(TaskRepository taskRepository, TodoStorageService storageService) {
        this.taskRepository = taskRepository;
        this.storageService = storageService;
    }

    // صفحه اصلی
    @GetMapping("/")
    public String index(Model model) {
        List<Task> tasks = taskRepository.findAll();
        // دسته‌بندی تسک‌ها برای ویو
        model.addAttribute("todos", tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).collect(Collectors.toList()));
        model.addAttribute("inProgress", tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).collect(Collectors.toList()));
        model.addAttribute("dones", tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).collect(Collectors.toList()));
        return "index";
    }

    // API برای اضافه کردن تسک
    @PostMapping("/api/tasks")
    @ResponseBody
    public ResponseEntity<Task> addTask(@RequestBody TaskRequest req) {
        Task task = new Task();
        task.setTitle(req.getTitle());
        task.setStatus(req.getStatus());
        task.setReminderTime(req.getReminderTime());
        return ResponseEntity.ok(storageService.save(task));
    }

    // API برای تغییر وضعیت (Drag & Drop support)
    @PatchMapping("/api/tasks/{id}/status")
    @ResponseBody
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam TaskStatus status) {
        Task task = taskRepository.findById(id).orElseThrow();
        task.setStatus(status);
        storageService.save(task);
        return ResponseEntity.ok().build();
    }

    // API برای ثبت کارکرد (Worklog)
    @PostMapping("/api/tasks/{id}/worklog")
    @ResponseBody
    public ResponseEntity<Task> addWorkLog(@PathVariable Long id, @RequestBody WorkLogRequest req) {
        Task task = taskRepository.findById(id).orElseThrow();
        WorkLog log = new WorkLog(null, req.getMinutes(), req.getDescription(), LocalDateTime.now());
        task.getWorkLogs().add(log);
        return ResponseEntity.ok(storageService.save(task));
    }

    // API برای حذف تسک
    @DeleteMapping("/api/tasks/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        storageService.delete(id);
        return ResponseEntity.ok().build();
    }
}
