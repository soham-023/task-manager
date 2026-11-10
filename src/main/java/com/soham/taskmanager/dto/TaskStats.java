package com.soham.taskmanager.dto;

/**
 * Aggregate task counts shown on the dashboard, returned by {@code GET /api/tasks/stats}.
 *
 * <p>Serialized by Jackson as {@code {"total":..,"todo":..,"inProgress":..,"done":..,"overdue":..}},
 * the same shape the previous {@code Map<String, Long>} produced, but with a fixed field
 * order and no string keys to mistype.</p>
 *
 * @param total      number of tasks
 * @param todo       tasks in {@code TODO}
 * @param inProgress tasks in {@code IN_PROGRESS}
 * @param done       tasks in {@code DONE}
 * @param overdue    tasks not {@code DONE} whose due date is before today
 */
public record TaskStats(long total, long todo, long inProgress, long done, long overdue) {
}
