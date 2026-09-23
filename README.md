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
- **Comprehensive Automated Tests**: 14 Spring Boot integration tests verifying all REST endpoints and edge cases.

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
│       └── TaskControllerTest.java                 # Integration test suite (13 tests)
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
Execute the integration test suite:

```bash
mvn test
```

---

## 🔌 REST API Reference

| Method | Endpoint | Description | Query Parameters |
|---|---|---|---|
| `GET` | `/api/tasks` | Fetch tasks | `status`, `priority`, `search` |
| `GET` | `/api/tasks/{id}` | Fetch a single task by ID | - |
| `POST` | `/api/tasks` | Create a new task | - |
| `PUT` | `/api/tasks/{id}` | Update an existing task | - |
| `DELETE` | `/api/tasks/{id}` | Delete a task by ID | - |
| `PATCH` | `/api/tasks/{id}/status`| Quick status update | - |

### Example Payload (`POST /api/tasks`)

```json
{
  "title": "Build user authentication",
  "description": "Implement JWT-based authentication in Spring Boot",
  "priority": "HIGH",
  "status": "TODO"
}
```
