package dev.rrezart.misconception.model;

/**
 * A misconception the detector believes a submission shows.
 *
 * @param misconceptionId catalogue id, for example "M03"
 * @param evidence        the part of the submission that supports the match
 * @param confidence      value between 0 and 1 reported by the model
 */
public record Match(String misconceptionId, String evidence, double confidence) {
}
