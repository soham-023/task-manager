package com.soham.taskmanager.service;

import com.soham.taskmanager.exception.ResourceNotFoundException;
import com.soham.taskmanager.model.Task;
import com.soham.taskmanager.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;


/**
 * Service layer containing business logic for Task operations.
 * Acts as an intermediary between the controller and repository.
 */
@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        
        this.taskRepository = taskRepository;
    }

    /**
     * Retrieve a page of tasks, optionally filtered by status, priority, category and a
     * title keyword. Any criterion may be {@code null}; a blank keyword is ignored.
     */
    public Page<Task> getAllTasks(Task.Status status, Task.Priority priority, Task.Category category,
                                  String keyword, Pageable pageable) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : escapeLikePattern(keyword.trim());
        return taskRepository.findWithFilters(status, priority, category, normalizedKeyword, pageable);
    }

    /**
     * Escape LIKE wildcards so a user's {@code %} or {@code _} is matched literally
     * instead of acting as a pattern. Uses {@code !} as the escape character, matching
     * the {@code ESCAPE '!'} clause in {@link TaskRepository#findWithFilters}.
     */
    static String escapeLikePattern(String keyword) {
        return keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    /**
     * Find a single task by its ID.
     *
     * @throws ResourceNotFoundException if no task exists with the given ID
     */
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    /**
     * Create a new task.
     *
     * @throws IllegalArgumentException if the due date is already in the past
     */
    public Task createTask(Task task) {
        // Only new tasks are checked: an existing task may legitimately become
        // overdue and still needs to be editable.
        if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Due date cannot be in the past");
        }
        return taskRepository.save(task);
    }

    /**
     * Update an existing task's fields.
     *
     * @throws ResourceNotFoundException if no task exists with the given ID
     */
    public Task updateTask(Long id, Task updatedTask) {
        Task existing = getTaskById(id);
        existing.setTitle(updatedTask.getTitle());
        existing.setDescription(updatedTask.getDescription());
        existing.setPriority(updatedTask.getPriority());
        existing.setStatus(updatedTask.getStatus());
        existing.setDueDate(updatedTask.getDueDate());
        existing.setCategory(updatedTask.getCategory());
        return taskRepository.save(existing);
    }

    /**
     * Delete a task by its ID.
     *
     * @throws ResourceNotFoundException if no task exists with the given ID
     */
    public void deleteTask(Long id) {
        Task existing = getTaskById(id);
        taskRepository.delete(existing);
    }

    /**
     * Update only the status of a task.
     *
     * @throws ResourceNotFoundException if no task exists with the given ID
     */
    public Task updateTaskStatus(Long id, Task.Status status) {
        Task existing = getTaskById(id);
        existing.setStatus(status);
        return taskRepository.save(existing);
        
    }

    /**
     * Compute aggregate metrics across all tasks for dashboard statistics.
     */
    public Map<String, Long> getTaskStats() {
        Map<String, Long> stats = new HashMap<>();
        
        long total = taskRepository.count();
        long todo = taskRepository.countByStatus(Task.Status.TODO);
        long inProgress = taskRepository.countByStatus(Task.Status.IN_PROGRESS);
        long done = taskRepository.countByStatus(Task.Status.DONE);
        long overdue = taskRepository.countOverdueTasks(LocalDate.now());

        stats.put("total", total);
        stats.put("todo", todo);
        stats.put("inProgress", inProgress);
        stats.put("done", done);
        stats.put("overdue", overdue);
        return stats;
    }
}
