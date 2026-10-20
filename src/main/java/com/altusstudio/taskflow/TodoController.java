package com.altusstudio.taskflow;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class TodoController {
    private final TaskRepository taskRepository;
    private final TodoStorageService storageService;
    private final DesktopNotificationService notifications;

    public TodoController(TaskRepository taskRepository, TodoStorageService storageService,
                          DesktopNotificationService notifications) {
        this.taskRepository = taskRepository;
        this.storageService = storageService;
        this.notifications = notifications;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("desktopNotificationsAvailable", notifications.isAvailable());
        List<Task> tasks = taskRepository.findAll();
        model.addAttribute("todos", tasks.stream().filter(task -> task.getStatus() == TaskStatus.TODO).collect(Collectors.toList()));
        model.addAttribute("inProgress", tasks.stream().filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS).collect(Collectors.toList()));
        model.addAttribute("dones", tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).collect(Collectors.toList()));
        return "index";
    }

    @GetMapping("/worklog-report")
    public String worklogReport(@RequestParam(required = false) LocalDate from,
                                @RequestParam(required = false) LocalDate to, Model model) {
        LocalDate today = LocalDate.now();
        LocalDate start = from == null ? today.withDayOfMonth(1) : from;
        LocalDate end = to == null ? today : to;
        if (end.isBefore(start)) throw new IllegalArgumentException("End date must not precede start date.");

        List<WorklogReportEntry> entries = findWorklogEntries(start, end);
        model.addAttribute("entries", entries);
        model.addAttribute("totalMinutes", entries.stream().mapToInt(WorklogReportEntry::durationMinutes).sum());
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        return "worklog-report";
    }

    @GetMapping("/worklog-report/export")
    @ResponseBody
    public ResponseEntity<byte[]> exportWorklogReport(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        if (to.isBefore(from)) throw new IllegalArgumentException("End date must not precede start date.");
        List<WorklogReportEntry> entries = findWorklogEntries(from, to);
        int totalMinutes = entries.stream().mapToInt(WorklogReportEntry::durationMinutes).sum();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=worklog-report.xlsx")
                .body(WorklogExcelExporter.export(entries, totalMinutes));
    }

    private List<WorklogReportEntry> findWorklogEntries(LocalDate start, LocalDate end) {
        return taskRepository.findAll().stream()
                .flatMap(task -> task.getWorkLogs().stream()
                        .filter(log -> !log.getLoggedAt().toLocalDate().isBefore(start)
                                && !log.getLoggedAt().toLocalDate().isAfter(end))
                        .map(log -> new WorklogReportEntry(task.getId(), log.getId(), task.getTitle(),
                                log.getDurationMinutes(), log.getDescription(), log.getLoggedAt())))
                .sorted(Comparator.comparing(WorklogReportEntry::loggedAt).reversed())
                .toList();
    }

    @PostMapping("/api/tasks")
    @ResponseBody
    public ResponseEntity<Task> addTask(@RequestBody TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setLabel(request.getLabel());
        task.setStatus(request.getStatus());
        try {
            task.setReminderTime(parseReminderTime(request.getReminderTime()));
        } catch (DateTimeParseException exception) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(storageService.save(task));
    }

    private LocalDateTime parseReminderTime(String reminderTime) {
        return reminderTime == null || reminderTime.isBlank() ? null : LocalDateTime.parse(reminderTime);
    }

    @PutMapping("/api/tasks/{id}")
    @ResponseBody
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody TaskRequest request) {
        Task task = taskRepository.findById(id).orElseThrow();
        task.setTitle(request.getTitle());
        task.setLabel(request.getLabel());
        try {
            task.setReminderTime(parseReminderTime(request.getReminderTime()));
        } catch (DateTimeParseException exception) {
            return ResponseEntity.badRequest().build();
        }
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

    @PutMapping("/api/tasks/{taskId}/worklog/{workLogId}")
    @ResponseBody
    public ResponseEntity<Task> updateWorkLog(@PathVariable Long taskId, @PathVariable Long workLogId,
                                               @RequestBody WorkLogRequest request) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        WorkLog workLog = task.getWorkLogs().stream()
                .filter(log -> workLogId.equals(log.getId()))
                .findFirst()
                .orElseThrow();
        workLog.setDurationMinutes(request.getMinutes());
        workLog.setDescription(request.getDescription());
        return ResponseEntity.ok(storageService.save(task));
    }

    @DeleteMapping("/api/tasks/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        storageService.delete(id);
        return ResponseEntity.ok().build();
    }
}
