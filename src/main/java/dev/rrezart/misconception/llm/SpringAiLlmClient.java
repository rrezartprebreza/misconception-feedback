package dev.rrezart.misconception.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * Spring AI implementation. Messages are passed as message objects, not as prompt-template strings,
 * because student Java code contains curly braces that a template renderer would treat as variables.
 */
public class SpringAiLlmClient implements LlmClient {

    private final ChatClient chat;

    public SpringAiLlmClient(ChatClient.Builder builder) {
        this.chat = builder.build();
    }

    @Override
    public String complete(String system, String user) {
        return chat.prompt()
                .messages(new SystemMessage(system), new UserMessage(user))
                .call()
                .content();
    }

    @Override
    public <T> T completeStructured(String system, String user, Class<T> type) {
        return chat.prompt()
                .messages(new SystemMessage(system), new UserMessage(user))
                .call()
                .entity(type);
    }
}
