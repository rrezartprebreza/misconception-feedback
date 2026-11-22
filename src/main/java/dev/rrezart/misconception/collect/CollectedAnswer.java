package dev.rrezart.misconception.collect;

/**
 * One anonymised student answer collected for later labelling. The code and explanation have
 * already been through the anonymiser.
 */
public record CollectedAnswer(String id,
                              String collectedAt,
                              String participantId,
                              String taskId,
                              String task,
                              String language,
                              String code,
                              String explanation) {
}
