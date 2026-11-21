package dev.rrezart.misconception.catalogue;

/**
 * One catalogue entry: the false belief, how it typically shows up and the corrective explanation.
 */
public record Misconception(
        String id,
        String name,
        String topic,
        String belief,
        String pattern,
        String correction) {
}
