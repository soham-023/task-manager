# Task Manager — Full-Stack Java Application

A clean, modern, and well-structured task management application built with **Spring Boot 3** on the backend and a responsive **Vanilla HTML/CSS/JavaScript** frontend.

---

## 🚀 Features

- **Full CRUD Operations**: Create, read, update, and delete tasks.
- **Due Dates & Overdue Alerts**: Set task deadlines with automatic badges (`🚨 Overdue`, `⏳ Due Today`, `📅 Due: Date`) and overdue dashboard counters.
- **Quick Status Toggle**: Mark tasks as completed or reopen them with a single click.
- **Live Search & Filters**: Search tasks by title and filter by status (`TODO`, `IN_PROGRESS`, `DONE`) and priority (`LOW`, `MEDIUM`, `HIGH`).
- **Real-time Statistics**: Dashboard metrics tracking total, to-do, in-progress, completed, and overdue tasks.
- **Clean Layered Architecture**: Controller → Service → Repository → Model with DTO validation and centralized error handling.
- **Embedded Database**: H2 in-memory SQL database with web console enabled for easy inspection.
- **Responsive UI**: Pure CSS (no heavy framework dependencies), modern cards, modal dialogs, and toast notifications.
- **Comprehensive Automated Tests**: Spring Boot integration tests for every REST endpoint plus fast Mockito unit tests for the service layer, run on every push by GitHub Actions CI.

---

## 🛠️ Tech Stack

- **Backend**: Java 17, Spring Boot 3.2.5, Spring Data JPA, Hibernate, Jakarta Validation
- **Database**: H2 in-memory database
- **Frontend**: HTML5, Modern CSS3 (Grid & Flexbox), Vanilla JavaScript (ES6+ Fetch API)
- **Build Tool**: Apache Maven
- **Testing**: Spring Boot Test, MockMvc, JUnit 5, Hamcrest
  

---

## 📂 Project Structure

```
taskmanager/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/soham/taskmanager/
│   │   │   ├── TaskManagerApplication.java        # Application entry point
│   │   │   ├── controller/
│   │   │   │   └── TaskController.java            # REST API endpoints
│   │   │   ├── service/
│   │   │   │   └── TaskService.java               # Business logic layer
│   │   │   ├── repository/
│   │   │   │   └── TaskRepository.java            # Spring Data JPA queries
│   │   │   ├── model/
│   │   │   │   └── Task.java                      # JPA Entity with Priority/Status enums
│   │   │   ├── dto/
│   │   │   │   ├── StatusUpdateRequest.java       # Validated body for PATCH /status
│   │   │   │   └── TaskStats.java                 # Dashboard counts returned by /stats
│   │   │   └── exception/
│   │   │       ├── ResourceNotFoundException.java  # HTTP 404 handler
│   │   │       └── GlobalExceptionHandler.java     # Centralized @ControllerAdvice
│   │   └── resources/
│   │       ├── application.properties              # Server, H2 & JPA settings
│   │       └── static/                             # Frontend assets
│   │           ├── index.html                      # UI layout & modal
│   │           ├── css/
│   │           │   └── style.css                   # Custom modern styles
│   │           └── js/
│   │               └── app.js                      # REST API client & UI bindings
│   └── test/java/com/soham/taskmanager/
│       ├── TaskControllerTest.java                 # REST API integration tests (MockMvc)
│       └── service/
│           └── TaskServiceTest.java                # Service-layer unit tests (Mockito)
```

---

## 🏃 Getting Started

### Prerequisites
- **Java 17** or later (`java -version`)
- **Maven 3.6+** (`mvn -version`)

### 1. Run the Application
Navigate to the project root and start the Spring Boot application:

```bash
cd taskmanager
mvn spring-boot:run
```

### 2. Access the Application
- **Frontend Application**: Open [http://localhost:8080](http://localhost:8080) in your web browser.
- **H2 Database Console**: Open [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - **JDBC URL**: `jdbc:h2:mem:taskdb`
  - **User Name**: `sa`
  - **Password**: *(leave blank)*

### 3. Run Automated Tests
Execute the full test suite (integration + unit tests):

```bash
mvn test
```

---

## 🔌 REST API Reference

| Method | Endpoint | Description | Query Parameters |
|---|---|---|---|
| `GET` | `/api/tasks` | Fetch a page of tasks | `status`, `priority`, `category`, `search`, `page`, `size`, `sortBy`, `sortDir` |
| `GET` | `/api/tasks/stats` | Dashboard counts: `total`, `todo`, `inProgress`, `done`, `overdue` | - |
| `GET` | `/api/tasks/{id}` | Fetch a single task by ID | - |
| `POST` | `/api/tasks` | Create a new task | - |
| `PUT` | `/api/tasks/{id}` | Update an existing task | - |
| `DELETE` | `/api/tasks/{id}` | Delete a task by ID | - |
| `PATCH` | `/api/tasks/{id}/status`| Quick status update | - |

### Query Parameters for `GET /api/tasks`

| Parameter | Allowed values | Default |
|---|---|---|
| `status` | `TODO`, `IN_PROGRESS`, `DONE` | all |
| `priority` | `LOW`, `MEDIUM`, `HIGH` | all |
| `category` | `WORK`, `PERSONAL`, `STUDY`, `FINANCE`, `OTHER` | all |
| `search` | case-insensitive keyword matched against the title (combined with the filters above) | - |
| `page` | 0-indexed page number, `>= 0` | `0` |
| `size` | page size, `1`–`100` | `6` |
| `sortBy` | `createdAt`, `updatedAt`, `dueDate`, `priority`, `status`, `title` | `createdAt` |
| `sortDir` | `asc`, `desc` | `desc` |

The response is a Spring Data page: tasks are in `content`, with `totalElements`, `totalPages` and `number` (current page).

### Example Payload (`POST /api/tasks`)

```json
{
  "title": "Build user authentication",
  "description": "Implement JWT-based authentication in Spring Boot",
  "priority": "HIGH",
  "status": "TODO",
  "category": "WORK",
  "dueDate": "2026-12-01"
}
```

`title` is required (max 100 chars); `description` is optional (max 500 chars). `priority`, `status` and `category` default to `MEDIUM`, `TODO` and `OTHER`. A new task's `dueDate` must be today or later; updates may keep a date that has since passed.

### Example Requests

```bash
# Create a task
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title": "Write report", "priority": "HIGH", "dueDate": "2026-12-01"}'

# High-priority work tasks, earliest due date first, 10 per page
curl "http://localhost:8080/api/tasks?priority=HIGH&category=WORK&sortBy=dueDate&sortDir=asc&size=10"

# Mark task 1 as done
curl -X PATCH http://localhost:8080/api/tasks/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "DONE"}'
```

### Error Responses

Errors are returned as JSON with a consistent shape:

```json
{
  "timestamp": "2026-11-08T22:30:00.123",
  "status": 404,
  "error": "Not Found",
  "message": "Task not found with id: 42"
}
```

| Status | When |
|---|---|
| `400` | Validation failure (with a `fieldErrors` map), malformed JSON, unknown enum value, or invalid paging/sorting parameter |
| `404` | No task exists with the given ID |
| `500` | Unexpected server error |
