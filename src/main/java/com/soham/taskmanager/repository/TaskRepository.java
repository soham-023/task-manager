package com.soham.taskmanager.repository;

import com.soham.taskmanager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Task entities.
 * Spring Data JPA automatically provides CRUD implementations at runtime.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Find tasks by status.
     */
    List<Task> findByStatus(Task.Status status);

    /**
     * Find tasks by priority.
     */
    List<Task> findByPriority(Task.Priority priority);

    /**
     * Find tasks by both status and priority.
     */
    List<Task> findByStatusAndPriority(Task.Status status, Task.Priority priority);

    /**
     * Search tasks by title (case-insensitive, partial match).
     */
    @Query("SELECT t FROM Task t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Task> searchByTitle(@Param("keyword") String keyword);
}
