package com.soham.taskmanager.controller;

import com.soham.taskmanager.model.Task;
import com.soham.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing Task management endpoints.
 * All endpoints are prefixed with /api/tasks.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * GET /api/tasks
     * Retrieve all tasks. Supports optional filtering by status, priority, and search keyword.
     *
     * Query params:
     *   - status:   TODO | IN_PROGRESS | DONE
     *   - priority: LOW | MEDIUM | HIGH
     *   - search:   keyword to search titles
     */
    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks(
            @RequestParam(required = false) Task.Status status,
            @RequestParam(required = false) Task.Priority priority,
            @RequestParam(required = false) String search) {

        List<Task> tasks;
        if (search != null && !search.isBlank()) {
            tasks = taskService.searchTasks(search.trim());
        } else {
            tasks = taskService.getAllTasks(status, priority);
        }
        return ResponseEntity.ok(tasks);
    }

    /**
     * GET /api/tasks/{id}
     * Retrieve a single task by its ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        Task task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    /**
     * POST /api/tasks
     * Create a new task. Request body must pass validation.
     */
    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody Task task) {
        Task created = taskService.createTask(task);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * PUT /api/tasks/{id}
     * Update an existing task entirely. Request body must pass validation.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @Valid @RequestBody Task task) {
        Task updated = taskService.updateTask(id, task);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/tasks/{id}
     * Delete a task by its ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/tasks/{id}/status
     * Update only the status of a task.
     * Request body: { "status": "TODO" | "IN_PROGRESS" | "DONE" }
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateTaskStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String statusStr = body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }

        Task.Status status;
        try {
            status = Task.Status.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + statusStr +
                    ". Must be one of: TODO, IN_PROGRESS, DONE");
        }

        Task updated = taskService.updateTaskStatus(id, status);
        return ResponseEntity.ok(updated);
    }
}
