package dev.rrezart.misconception.web;

/** {@code language} is optional; without it the instance's default language is used. */
public record StudyRequest(String participantId, String taskId, String code, String explanation,
                           boolean consent, String language) {
}
