package dev.rrezart.misconception.web;

public record StudyRequest(String participantId, String taskId, String code, String explanation, boolean consent) {
}
