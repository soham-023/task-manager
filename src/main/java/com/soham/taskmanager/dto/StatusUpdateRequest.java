package com.soham.taskmanager.dto;

import com.soham.taskmanager.model.Task;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code PATCH /api/tasks/{id}/status}, e.g. {@code {"status": "DONE"}}.
 *
 * <p>Jackson binds the status straight to {@link Task.Status} (case-insensitively), so
 * unknown values are rejected by the global exception handler with the allowed list.</p>
 *
 * @param status the new workflow status; required
 */
public record StatusUpdateRequest(@NotNull(message = "Status is required") Task.Status status) {
}
