package dev.rrezart.misconception.model;

import java.util.List;

/**
 * Structured output the detector asks the model to return.
 */
public record DetectionResponse(List<Match> matches) {
}
