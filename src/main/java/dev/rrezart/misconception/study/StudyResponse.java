package dev.rrezart.misconception.study;

/**
 * What a participant sees. It deliberately omits the condition and the detector output so that
 * participants stay blind to which feedback type they received.
 */
public record StudyResponse(String feedback, int attempt) {
}
