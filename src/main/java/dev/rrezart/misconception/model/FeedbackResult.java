package dev.rrezart.misconception.model;

import java.util.List;

public record FeedbackResult(FeedbackCondition condition, String feedback, List<Match> matches) {
}
