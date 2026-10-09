package com.soham.taskmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soham.taskmanager.model.Task;
import com.soham.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Task REST API.
 * Uses the full Spring context with an in-memory H2 database.
 */


@SpringBootTest
@AutoConfigureMockMvc
    
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    // --- Helper ---
    
    
    private Task createSampleTask(String title, Task.Priority priority, Task.Status status) {
        Task task = new Task(title, "Description for " + title, priority, status);
        return taskRepository.save(task);
        
    }

    // ==========================================
    // GET /api/tasks
    // ==========================================

    @Test
    void getAllTasks_returnsEmptyList_whenNoTasks() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    void getAllTasks_returnsAllTasks() throws Exception {
        createSampleTask("Task 1", Task.Priority.LOW, Task.Status.TODO);
        createSampleTask("Task 2", Task.Priority.HIGH, Task.Status.DONE);

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    void getAllTasks_filterByStatus() throws Exception {
        createSampleTask("Task A", Task.Priority.LOW, Task.Status.TODO);
        createSampleTask("Task B", Task.Priority.HIGH, Task.Status.DONE);

        mockMvc.perform(get("/api/tasks").param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("Task A")));
    }

    @Test
    void getAllTasks_filterByPriority() throws Exception {
        createSampleTask("Low Task", Task.Priority.LOW, Task.Status.TODO);
        createSampleTask("High Task", Task.Priority.HIGH, Task.Status.TODO);

        mockMvc.perform(get("/api/tasks").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("High Task")));
    }

    @Test
    void getAllTasks_searchByTitle() throws Exception {
        createSampleTask("Buy groceries", Task.Priority.MEDIUM, Task.Status.TODO);
        createSampleTask("Read book", Task.Priority.LOW, Task.Status.TODO);

        mockMvc.perform(get("/api/tasks").param("search", "grocer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("Buy groceries")));
    }

    @Test
    void getAllTasks_filterByCategory() throws Exception {
        Task t1 = new Task("Work Task", "Desc", Task.Priority.HIGH, Task.Status.TODO, null, Task.Category.WORK);
        Task t2 = new Task("Personal Task", "Desc", Task.Priority.LOW, Task.Status.TODO, null, Task.Category.PERSONAL);
        taskRepository.save(t1);
        taskRepository.save(t2);

        mockMvc.perform(get("/api/tasks").param("category", "WORK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("Work Task")))
                .andExpect(jsonPath("$.content[0].category", is("WORK")));
    }

    @Test
    void getAllTasks_pagination() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createSampleTask("Task " + i, Task.Priority.LOW, Task.Status.TODO);
        }

        mockMvc.perform(get("/api/tasks")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(5)))
                .andExpect(jsonPath("$.totalPages", is(3)))
                .andExpect(jsonPath("$.number", is(0)));
    }

    @Test
    void getAllTasks_sorting() throws Exception {
        createSampleTask("Alpha Task", Task.Priority.LOW, Task.Status.TODO);
        createSampleTask("Zeta Task", Task.Priority.HIGH, Task.Status.TODO);

        mockMvc.perform(get("/api/tasks")
                        .param("sortBy", "title")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title", is("Alpha Task")))
                .andExpect(jsonPath("$.content[1].title", is("Zeta Task")));

        mockMvc.perform(get("/api/tasks")
                        .param("sortBy", "title")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title", is("Zeta Task")))
                .andExpect(jsonPath("$.content[1].title", is("Alpha Task")));
    }

    @Test
    void getTaskStats_returnsCounts() throws Exception {
        createSampleTask("Task 1", Task.Priority.LOW, Task.Status.TODO);
        createSampleTask("Task 2", Task.Priority.MEDIUM, Task.Status.IN_PROGRESS);
        createSampleTask("Task 3", Task.Priority.HIGH, Task.Status.DONE);

        // Overdue task (yesterday)
        Task overdueTask = new Task("Overdue", "Desc", Task.Priority.HIGH, Task.Status.TODO, LocalDate.now().minusDays(1));
        taskRepository.save(overdueTask);

        mockMvc.perform(get("/api/tasks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(4)))
                .andExpect(jsonPath("$.todo", is(2)))
                .andExpect(jsonPath("$.inProgress", is(1)))
                .andExpect(jsonPath("$.done", is(1)))
                .andExpect(jsonPath("$.overdue", is(1)));
    }

    // ==========================================
    // GET /api/tasks/{id}
    // ==========================================

    @Test
    void getTaskById_returnsTask() throws Exception {
        Task task = createSampleTask("My Task", Task.Priority.MEDIUM, Task.Status.TODO);

        mockMvc.perform(get("/api/tasks/" + task.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("My Task")))
                .andExpect(jsonPath("$.priority", is("MEDIUM")));
    }

    @Test
    void getTaskById_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // POST /api/tasks
    // ==========================================

    @Test
    void createTask_returnsCreatedTask() throws Exception {
        Task newTask = new Task("New Task", "A description", Task.Priority.HIGH, Task.Status.TODO);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("New Task")))
                .andExpect(jsonPath("$.priority", is("HIGH")));
    }

    @Test
    void createTask_returns400_whenTitleBlank() throws Exception {
        Task invalid = new Task("", null, Task.Priority.LOW, Task.Status.TODO);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // PUT /api/tasks/{id}
    // ==========================================

    @Test
    void createTask_withDueDate_returnsCreatedTaskWithDueDate() throws Exception {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Task newTask = new Task("Task with Due Date", "Details", Task.Priority.HIGH, Task.Status.TODO, tomorrow);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.dueDate", is(tomorrow.toString())));
    }

    @Test
    void createTask_withCategory_returnsCreatedTaskWithCategory() throws Exception {
        Task newTask = new Task("Study Spring", "Learn Spring Boot 3", Task.Priority.MEDIUM, Task.Status.TODO, null, Task.Category.STUDY);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.category", is("STUDY")));
    }

    @Test
    void updateTask_updatesAndReturnsTask() throws Exception {
        Task task = createSampleTask("Old Title", Task.Priority.LOW, Task.Status.TODO);

        LocalDate nextWeek = LocalDate.now().plusWeeks(1);
        Task updated = new Task("Updated Title", "Updated desc", Task.Priority.HIGH, Task.Status.IN_PROGRESS, nextWeek);

        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.dueDate", is(nextWeek.toString())));
    }

    // ==========================================
    // DELETE /api/tasks/{id}
    // ==========================================

    @Test
    void deleteTask_returns204() throws Exception {
        Task task = createSampleTask("To Delete", Task.Priority.LOW, Task.Status.TODO);

        mockMvc.perform(delete("/api/tasks/" + task.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + task.getId()))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // PATCH /api/tasks/{id}/status
    // ==========================================

    @Test
    void updateTaskStatus_updatesStatus() throws Exception {
        Task task = createSampleTask("Status Task", Task.Priority.MEDIUM, Task.Status.TODO);

        mockMvc.perform(patch("/api/tasks/" + task.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DONE")));
    }

    @Test
    void updateTaskStatus_returns400_forInvalidStatus() throws Exception {
        Task task = createSampleTask("Status Task", Task.Priority.MEDIUM, Task.Status.TODO);

        mockMvc.perform(patch("/api/tasks/" + task.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }
}
