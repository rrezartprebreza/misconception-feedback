package dev.rrezart.misconception.model;

/**
 * One student answer: the task text, the Java code and a short written explanation.
 */
public record Submission(String id, String task, String code, String explanation) {
}
