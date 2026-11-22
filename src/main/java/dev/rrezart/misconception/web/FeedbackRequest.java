package dev.rrezart.misconception.web;

import dev.rrezart.misconception.model.FeedbackCondition;

/** {@code language} is optional; without it the instance's default language is used. */
public record FeedbackRequest(String id, String task, String code, String explanation,
                              FeedbackCondition condition, String language) {
}
