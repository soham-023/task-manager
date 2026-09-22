package com.soham.taskmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soham.taskmanager.model.Task;
import com.soham.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllTasks_returnsAllTasks() throws Exception {
        createSampleTask("Task 1", Task.Priority.LOW, Task.Status.TODO);
        createSampleTask("Task 2", Task.Priority.HIGH, Task.Status.DONE);

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getAllTasks_filterByStatus() throws Exception {
        createSampleTask("Task A", Task.Priority.LOW, Task.Status.TODO);
        createSampleTask("Task B", Task.Priority.HIGH, Task.Status.DONE);

        mockMvc.perform(get("/api/tasks").param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Task A")));
    }

    @Test
    void getAllTasks_filterByPriority() throws Exception {
        createSampleTask("Low Task", Task.Priority.LOW, Task.Status.TODO);
        createSampleTask("High Task", Task.Priority.HIGH, Task.Status.TODO);

        mockMvc.perform(get("/api/tasks").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("High Task")));
    }

    @Test
    void getAllTasks_searchByTitle() throws Exception {
        createSampleTask("Buy groceries", Task.Priority.MEDIUM, Task.Status.TODO);
        createSampleTask("Read book", Task.Priority.LOW, Task.Status.TODO);

        mockMvc.perform(get("/api/tasks").param("search", "grocer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Buy groceries")));
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
    void updateTask_updatesAndReturnsTask() throws Exception {
        Task task = createSampleTask("Old Title", Task.Priority.LOW, Task.Status.TODO);

        Task updated = new Task("Updated Title", "Updated desc", Task.Priority.HIGH, Task.Status.IN_PROGRESS);

        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
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
