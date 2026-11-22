package dev.rrezart.misconception.study;

import dev.rrezart.misconception.model.FeedbackCondition;
import dev.rrezart.misconception.model.Match;

import java.util.List;

/**
 * One logged submission. Code and explanation are stored after anonymisation, exactly as the model
 * saw them. The condition and detector matches are logged here but never sent back to the participant.
 */
public record StudyRecord(
        String timestamp,
        String participantId,
        String taskId,
        int topic,
        int attempt,
        FeedbackCondition condition,
        String model,
        String code,
        String explanation,
        String feedback,
        List<Match> matches) {
}
