package com.soham.taskmanager.repository;

import com.soham.taskmanager.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Slice tests for the custom JPQL queries in {@link TaskRepository}, run against the
 * embedded H2 database with only the JPA layer loaded.
 */
@DataJpaTest
class TaskRepositoryTest {

    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20, Sort.by("title"));

    @Autowired
    private TaskRepository taskRepository;

    private final LocalDate today = LocalDate.of(2030, 6, 15);

    @BeforeEach
    void seed() {
        save("Write report", Task.Priority.HIGH, Task.Status.TODO, Task.Category.WORK, today.minusDays(2));
        save("Review report", Task.Priority.LOW, Task.Status.DONE, Task.Category.WORK, today.minusDays(5));
        save("Gym session", Task.Priority.MEDIUM, Task.Status.IN_PROGRESS, Task.Category.PERSONAL, today);
        save("Pay rent", Task.Priority.HIGH, Task.Status.TODO, Task.Category.FINANCE, null);
    }

    private void save(String title, Task.Priority priority, Task.Status status, Task.Category category, LocalDate due) {
        taskRepository.save(new Task(title, null, priority, status, due, category));
    }

    @Test
    void findWithFilters_withAllNulls_returnsEverything() {
        Page<Task> page = taskRepository.findWithFilters(null, null, null, null, FIRST_PAGE);
        assertEquals(4, page.getTotalElements());
    }

    @Test
    void findWithFilters_combinesCriteria() {
        Page<Task> page = taskRepository.findWithFilters(
                Task.Status.TODO, Task.Priority.HIGH, Task.Category.WORK, null, FIRST_PAGE);

        assertEquals(1, page.getTotalElements());
        assertEquals("Write report", page.getContent().get(0).getTitle());
    }

    @Test
    void findWithFilters_keywordIsCaseInsensitivePartialMatch() {
        Page<Task> page = taskRepository.findWithFilters(null, null, null, "REPORT", FIRST_PAGE);

        assertEquals(2, page.getTotalElements());
        assertEquals("Review report", page.getContent().get(0).getTitle());
        assertEquals("Write report", page.getContent().get(1).getTitle());
    }

    @Test
    void findWithFilters_appliesPaging() {
        Page<Task> page = taskRepository.findWithFilters(null, null, null, null, PageRequest.of(1, 3, Sort.by("title")));

        assertEquals(4, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(1, page.getNumberOfElements());
        assertEquals("Write report", page.getContent().get(0).getTitle());
    }

    @Test
    void countByStatus_countsEachStatus() {
        assertEquals(2, taskRepository.countByStatus(Task.Status.TODO));
        assertEquals(1, taskRepository.countByStatus(Task.Status.IN_PROGRESS));
        assertEquals(1, taskRepository.countByStatus(Task.Status.DONE));
    }

    @Test
    void countOverdueTasks_excludesDoneTasks_dueToday_andTasksWithoutDueDate() {
        // Only "Write report" counts: "Review report" is DONE, "Gym session" is due today,
        // "Pay rent" has no due date.
        assertEquals(1, taskRepository.countOverdueTasks(today));
    }

    @Test
    void save_setsCreatedAndUpdatedTimestamps() {
        Task saved = taskRepository.saveAndFlush(new Task("Timestamps", null, Task.Priority.LOW, Task.Status.TODO));

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }
}
