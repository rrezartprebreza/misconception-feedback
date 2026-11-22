package dev.rrezart.misconception.web;

import java.util.List;

/**
 * What the labelling page sees. The participant id is left out so the labeller cannot be swayed by who wrote
 * the answer. {@code labels} is null while the answer is unlabelled, and an empty list means "none".
 */
public record AdminAnswerView(String id, String taskId, String task, String language,
                              String code, String explanation, List<String> labels) {
}
