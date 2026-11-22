package dev.rrezart.misconception.web;

public record CollectRequest(String participantId, String taskId, String code, String explanation, boolean consent) {
}
