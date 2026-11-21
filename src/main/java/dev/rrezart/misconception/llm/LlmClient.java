package dev.rrezart.misconception.llm;

/**
 * Thin boundary around the model so the study logic can be tested without calling one.
 */
public interface LlmClient {

    String complete(String system, String user);

    <T> T completeStructured(String system, String user, Class<T> type);
}
