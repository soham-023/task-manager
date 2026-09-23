package com.soham.taskmanager.service;

import com.soham.taskmanager.exception.ResourceNotFoundException;
import com.soham.taskmanager.model.Task;
import com.soham.taskmanager.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
     * Retrieve all tasks, optionally filtered by status and/or priority.
     */
    public List<Task> getAllTasks(Task.Status status, Task.Priority priority) {
        if (status != null && priority != null) {
            return taskRepository.findByStatusAndPriority(status, priority);
        } else if (status != null) {
            return taskRepository.findByStatus(status);
        } else if (priority != null) {
            return taskRepository.findByPriority(priority);
        }
        return taskRepository.findAll();
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
}
