package dev.rrezart.misconception;

import dev.rrezart.misconception.llm.LlmClient;

/**
 * Test double: records the prompts it receives and returns canned output.
 */
public class FakeLlmClient implements LlmClient {

    public String lastSystem;
    public String lastUser;
    public int completeCalls;
    public Object structuredResponse;
    public String textResponse = "FEEDBACK";

    @Override
    public String complete(String system, String user) {
        completeCalls++;
        lastSystem = system;
        lastUser = user;
        return textResponse;
    }

    @Override
    public <T> T completeStructured(String system, String user, Class<T> type) {
        lastSystem = system;
        lastUser = user;
        return type.cast(structuredResponse);
    }
}
