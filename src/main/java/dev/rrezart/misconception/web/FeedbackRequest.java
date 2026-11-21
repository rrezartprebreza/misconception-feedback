package dev.rrezart.misconception.web;

import dev.rrezart.misconception.model.FeedbackCondition;

public record FeedbackRequest(String id, String task, String code, String explanation, FeedbackCondition condition) {
}
