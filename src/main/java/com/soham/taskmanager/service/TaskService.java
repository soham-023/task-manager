package com.soham.taskmanager.service;

import com.soham.taskmanager.exception.ResourceNotFoundException;
import com.soham.taskmanager.model.Task;
import com.soham.taskmanager.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
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
     * Retrieve tasks with pagination, sorting, and optional filtering by status, priority, and/or category.
     */
    public Page<Task> getAllTasks(Task.Status status, Task.Priority priority, Task.Category category, Pageable pageable) {
        return taskRepository.findWithFilters(status, priority, category, pageable);
    }

    /**
     * Retrieve all tasks, optionally filtered by status, priority, and/or category.
     */
    public List<Task> getAllTasks(Task.Status status, Task.Priority priority, Task.Category category) {
        if (status != null || priority != null || category != null) {
            return taskRepository.findWithFilters(status, priority, category);
        }
        return taskRepository.findAll();
    }

    /**
     * Retrieve all tasks, optionally filtered by status and/or priority.
     */
    public List<Task> getAllTasks(Task.Status status, Task.Priority priority) {
        return getAllTasks(status, priority, null);
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
     */
    public Task createTask(Task task) {
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
     * Search tasks by title keyword (case-insensitive).
     */
    public List<Task> searchTasks(String keyword) {
        return taskRepository.searchByTitle(keyword);
    }

    /**
     * Search tasks by title keyword with pagination.
     */
    public Page<Task> searchTasks(String keyword, Pageable pageable) {
        return taskRepository.searchByTitle(keyword, pageable);
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
