package com.amirdehi.simpletodo;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
class WorkLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int durationMinutes;
    private String description;
    private LocalDateTime loggedAt;
}

// --- REPOSITORIES ---
interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByOrderByStatusAsc();
}

// --- DTOs ---
@Data
class TaskRequest {
    private String title;
    private TaskStatus status;
    private LocalDateTime reminderTime;
}

@Data
class WorkLogRequest {
    private int minutes;
    private String description;
}

// --- CONTROLLER ---
@Controller
@RequiredArgsConstructor
class TodoController {

    private final TaskRepository taskRepository;

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
        return ResponseEntity.ok(taskRepository.save(task));
    }

    // API برای تغییر وضعیت (Drag & Drop support)
    @PatchMapping("/api/tasks/{id}/status")
    @ResponseBody
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam TaskStatus status) {
        Task task = taskRepository.findById(id).orElseThrow();
        task.setStatus(status);
        taskRepository.save(task);
        return ResponseEntity.ok().build();
    }

    // API برای ثبت کارکرد (Worklog)
    @PostMapping("/api/tasks/{id}/worklog")
    @ResponseBody
    public ResponseEntity<Task> addWorkLog(@PathVariable Long id, @RequestBody WorkLogRequest req) {
        Task task = taskRepository.findById(id).orElseThrow();
        WorkLog log = new WorkLog(null, req.getMinutes(), req.getDescription(), LocalDateTime.now());
        task.getWorkLogs().add(log);
        return ResponseEntity.ok(taskRepository.save(task));
    }

    // API برای حذف تسک
    @DeleteMapping("/api/tasks/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
