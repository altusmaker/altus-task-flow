package com.amirdehi.simpletodo;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class TodoController {
    private final TaskRepository taskRepository;
    private final TodoStorageService storageService;

    public TodoController(TaskRepository taskRepository, TodoStorageService storageService) {
        this.taskRepository = taskRepository;
        this.storageService = storageService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Task> tasks = taskRepository.findAll();
        model.addAttribute("todos", tasks.stream().filter(task -> task.getStatus() == TaskStatus.TODO).collect(Collectors.toList()));
        model.addAttribute("inProgress", tasks.stream().filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS).collect(Collectors.toList()));
        model.addAttribute("dones", tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).collect(Collectors.toList()));
        return "index";
    }

    @PostMapping("/api/tasks")
    @ResponseBody
    public ResponseEntity<Task> addTask(@RequestBody TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setStatus(request.getStatus());
        task.setReminderTime(request.getReminderTime());
        return ResponseEntity.ok(storageService.save(task));
    }

    @PatchMapping("/api/tasks/{id}/status")
    @ResponseBody
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam TaskStatus status) {
        Task task = taskRepository.findById(id).orElseThrow();
        task.setStatus(status);
        storageService.save(task);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/tasks/{id}/worklog")
    @ResponseBody
    public ResponseEntity<Task> addWorkLog(@PathVariable Long id, @RequestBody WorkLogRequest request) {
        Task task = taskRepository.findById(id).orElseThrow();
        task.getWorkLogs().add(new WorkLog(null, request.getMinutes(), request.getDescription(), LocalDateTime.now()));
        return ResponseEntity.ok(storageService.save(task));
    }

    @DeleteMapping("/api/tasks/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        storageService.delete(id);
        return ResponseEntity.ok().build();
    }
}
