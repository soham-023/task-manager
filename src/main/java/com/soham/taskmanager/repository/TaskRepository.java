package com.soham.taskmanager.repository;

import com.soham.taskmanager.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * Repository interface for Task entities.
 * Spring Data JPA automatically provides CRUD implementations at runtime.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Flexible multi-criteria filter query with pagination and sorting.
     */
    @Query("SELECT t FROM Task t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(:category IS NULL OR t.category = :category)")
    Page<Task> findWithFilters(@Param("status") Task.Status status,
                               @Param("priority") Task.Priority priority,
                               @Param("category") Task.Category category,
                               Pageable pageable);

    /**
     * Search tasks by title (case-insensitive, partial match) with pagination.
     */
    @Query("SELECT t FROM Task t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Task> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Count tasks by status for dashboard statistics.
     */
    long countByStatus(Task.Status status);

    /**
     * Count tasks whose status differs from {@code excludedStatus} and whose
     * due date is strictly before {@code today}.
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.status <> :excludedStatus " +
           "AND t.dueDate IS NOT NULL AND t.dueDate < :today")
    long countByStatusNotAndDueDateBefore(@Param("excludedStatus") Task.Status excludedStatus,
                                          @Param("today") LocalDate today);

    /**
     * Count overdue tasks (status is not DONE and dueDate is strictly before today).
     * Binds the enum as a parameter instead of a hard-coded string literal,
     * so renaming the enum constant cannot silently break the query.
     */
    default long countOverdueTasks(LocalDate today) {
        return countByStatusNotAndDueDateBefore(Task.Status.DONE, today);
    }
}
