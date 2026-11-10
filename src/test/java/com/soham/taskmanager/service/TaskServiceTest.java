package com.soham.taskmanager.service;

import com.soham.taskmanager.exception.ResourceNotFoundException;
import com.soham.taskmanager.model.Task;
import com.soham.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskService} using a mocked {@link TaskRepository}.
 * These run without a Spring context, so they are fast and isolate business logic.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository);
    }

    private static Task task(Long id, String title) {
        Task t = new Task(title, "desc", Task.Priority.MEDIUM, Task.Status.TODO);
        t.setId(id);
        return t;
    }

    @Test
    void createTask_rejectsDueDateInThePast_withoutSaving() {
        Task t = new Task("Late", null, Task.Priority.LOW, Task.Status.TODO, LocalDate.now().minusDays(1));

        assertThrows(IllegalArgumentException.class, () -> taskService.createTask(t));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_acceptsDueDateToday() {
        Task t = new Task("Today", null, Task.Priority.LOW, Task.Status.TODO, LocalDate.now());
        when(taskRepository.save(t)).thenReturn(t);

        assertSame(t, taskService.createTask(t));
    }

    @Test
    void getTaskById_returnsTask_whenPresent() {
        Task existing = task(1L, "Existing");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertSame(existing, taskService.getTaskById(1L));
    }

    @Test
    void getTaskById_throwsNotFound_whenMissing() {
        when(taskRepository.findById(42L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> taskService.getTaskById(42L));
        assertEquals("Task not found with id: 42", ex.getMessage());
    }

    @Test
    void updateTask_copiesAllEditableFields() {
        Task existing = task(1L, "Old");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate due = LocalDate.of(2030, 1, 15);
        Task changes = new Task("New", "New desc", Task.Priority.HIGH, Task.Status.IN_PROGRESS,
                due, Task.Category.WORK);

        Task result = taskService.updateTask(1L, changes);

        assertEquals(1L, result.getId());
        assertEquals("New", result.getTitle());
        assertEquals("New desc", result.getDescription());
        assertEquals(Task.Priority.HIGH, result.getPriority());
        assertEquals(Task.Status.IN_PROGRESS, result.getStatus());
        assertEquals(due, result.getDueDate());
        assertEquals(Task.Category.WORK, result.getCategory());
    }

    @Test
    void updateTask_throwsNotFound_andDoesNotSave_whenMissing() {
        when(taskRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.updateTask(7L, task(null, "x")));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void deleteTask_deletesExistingTask() {
        Task existing = task(3L, "Delete me");
        when(taskRepository.findById(3L)).thenReturn(Optional.of(existing));

        taskService.deleteTask(3L);

        verify(taskRepository).delete(existing);
    }

    @Test
    void deleteTask_throwsNotFound_whenMissing() {
        when(taskRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTask(3L));
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void updateTaskStatus_changesOnlyStatus() {
        Task existing = task(5L, "Keep title");
        when(taskRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.updateTaskStatus(5L, Task.Status.DONE);

        ArgumentCaptor<Task> saved = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(saved.capture());
        assertEquals(Task.Status.DONE, saved.getValue().getStatus());
        assertEquals("Keep title", result.getTitle());
        assertEquals(Task.Priority.MEDIUM, result.getPriority());
    }

    @Test
    void getAllTasks_delegatesFiltersAndPageableToRepository() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Task> page = new PageImpl<>(List.of(task(1L, "a")), pageable, 1);
        when(taskRepository.findWithFilters(null, Task.Priority.HIGH, Task.Category.WORK, pageable)).thenReturn(page);

        assertSame(page, taskService.getAllTasks(null, Task.Priority.HIGH, Task.Category.WORK, pageable));
    }

    @Test
    void searchTasks_delegatesKeywordAndPageableToRepository() {
        Pageable pageable = PageRequest.of(1, 3);
        Page<Task> page = new PageImpl<>(List.of(), pageable, 0);
        when(taskRepository.searchByTitle("report", pageable)).thenReturn(page);

        assertSame(page, taskService.searchTasks("report", pageable));
    }

    @Test
    void getTaskStats_aggregatesRepositoryCounts() {
        when(taskRepository.count()).thenReturn(10L);
        when(taskRepository.countByStatus(Task.Status.TODO)).thenReturn(4L);
        when(taskRepository.countByStatus(Task.Status.IN_PROGRESS)).thenReturn(3L);
        when(taskRepository.countByStatus(Task.Status.DONE)).thenReturn(3L);
        when(taskRepository.countOverdueTasks(any(LocalDate.class))).thenReturn(2L);

        Map<String, Long> stats = taskService.getTaskStats();

        assertEquals(10L, stats.get("total"));
        assertEquals(4L, stats.get("todo"));
        assertEquals(3L, stats.get("inProgress"));
        assertEquals(3L, stats.get("done"));
        assertEquals(2L, stats.get("overdue"));
    }
}
