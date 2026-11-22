package dev.rrezart.misconception.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * Spring AI implementation. Messages are passed as message objects, not as prompt-template strings,
 * because student Java code contains curly braces that a template renderer would treat as variables.
 */
public class SpringAiLlmClient implements LlmClient {

    private final ChatClient chat;
    private final ChatOptions options;

    /** Uses the model from the application properties. */
    public SpringAiLlmClient(ChatClient.Builder builder) {
        this(builder, null);
    }

    /** Uses the given model name instead of the configured one (null or blank keeps the configured model). */
    public SpringAiLlmClient(ChatClient.Builder builder, String model) {
        this.chat = builder.build();
        this.options = (model == null || model.isBlank()) ? null : ChatOptions.builder().model(model).build();
    }

    private ChatClient.ChatClientRequestSpec request(String system, String user) {
        ChatClient.ChatClientRequestSpec spec = chat.prompt()
                .messages(new SystemMessage(system), new UserMessage(user));
        return options == null ? spec : spec.options(options);
    }

    @Override
    public String complete(String system, String user) {
        return request(system, user).call().content();
    }

    @Override
    public <T> T completeStructured(String system, String user, Class<T> type) {
        return request(system, user).call().entity(type);
    }
}
