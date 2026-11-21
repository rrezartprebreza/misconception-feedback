package dev.rrezart.misconception.eval;

import dev.rrezart.misconception.model.Submission;

import java.util.List;

/**
 * A submission with the misconception ids an instructor assigned to it. An empty list means the
 * answer shows none of the catalogue misconceptions.
 */
public record GoldItem(String id, String task, String code, String explanation, List<String> labels) {

    public Submission toSubmission() {
        return new Submission(id, task, code, explanation);
    }
}
