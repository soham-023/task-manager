package com.soham.taskmanager.controller;

import com.soham.taskmanager.dto.StatusUpdateRequest;
import com.soham.taskmanager.dto.TaskStats;
import com.soham.taskmanager.model.Task;
import com.soham.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Set;

/**
 * REST controller exposing Task management endpoints.
 * All endpoints are prefixed with /api/tasks.
 */
@RestController
@RequestMapping("/api/tasks")
    
public class TaskController {

    /** Fields clients are allowed to sort by; anything else is rejected with 400. */
    static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt", "updatedAt", "dueDate", "priority", "status", "title");

    /** Upper bound on page size to keep responses small. */
    static final int MAX_PAGE_SIZE = 100;

    private final TaskService taskService;
    

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
        
    }

    /**
     * GET /api/tasks
     * Retrieve tasks with server-side pagination, sorting, and optional filtering.
     *
     * Query params:
     *   - status:   TODO | IN_PROGRESS | DONE
     *   - priority: LOW | MEDIUM | HIGH
     *   - category: WORK | PERSONAL | STUDY | FINANCE | OTHER
     *   - search:   keyword to search titles (combined with the filters above)
     *   - page:     0-indexed page number (default 0)
     *   - size:     page size (default 6)
     *   - sortBy:   createdAt | dueDate | priority | title (default "createdAt")
     *   - sortDir:  asc | desc (default "desc")
     */
    @GetMapping
    public ResponseEntity<Page<Task>> getAllTasks(
            @RequestParam(required = false) Task.Status status,
            @RequestParam(required = false) Task.Priority priority,
        
            @RequestParam(required = false) Task.Category category,
        
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sortBy: " + sortBy +
                    ". Must be one of: " + String.join(", ", ALLOWED_SORT_FIELDS.stream().sorted().toList()));
        }
        if (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("Invalid sortDir: " + sortDir + ". Must be asc or desc");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        

        Page<Task> tasks = taskService.getAllTasks(status, priority, category, search, pageable);
        return ResponseEntity.ok(tasks);
    }

    /**
     * GET /api/tasks/stats
     * Return aggregate task counts (total, todo, inProgress, done, overdue) for the dashboard.
     */
    @GetMapping("/stats")
    public ResponseEntity<TaskStats> getTaskStats() {
        return ResponseEntity.ok(taskService.getTaskStats());
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
            @Valid @RequestBody StatusUpdateRequest request) {
        Task updated = taskService.updateTaskStatus(id, request.status());
        return ResponseEntity.ok(updated);
    }
}
