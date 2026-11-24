package dev.rrezart.misconception.evalbaseline;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Minimal chat model for recording eval answers from any OpenAI-compatible endpoint (Groq, or
 * Ollama at {@code http://localhost:11434}). It lives in the tests so recording works with either
 * Maven profile; the application itself keeps using the Spring AI starters.
 */
class OpenAiCompatibleChatModel implements ChatModel {

    private final RestClient http;
    private final String model;

    OpenAiCompatibleChatModel(String baseUrl, String apiKey, String model) {
        this.http = RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/+$", ""))
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.model = model;
    }

    /** Reads EVAL_BASE_URL, EVAL_API_KEY and EVAL_MODEL; the defaults point at a local Ollama. */
    static OpenAiCompatibleChatModel fromEnvironment() {
        String baseUrl = System.getenv().getOrDefault("EVAL_BASE_URL", "http://localhost:11434");
        String key = System.getenv().getOrDefault("EVAL_API_KEY", "ollama");
        String model = System.getenv().getOrDefault("EVAL_MODEL", "qwen2.5-coder:7b");
        return new OpenAiCompatibleChatModel(baseUrl, key, model);
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        List<Map<String, String>> messages = prompt.getInstructions().stream()
                .map(OpenAiCompatibleChatModel::toWire)
                .toList();
        JsonNode body = http.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", model, "temperature", 0, "messages", messages))
                .retrieve()
                .body(JsonNode.class);
        String text = body == null ? "" : body.path("choices").path(0).path("message").path("content").asText("");
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private static Map<String, String> toWire(Message message) {
        return Map.of("role", message.getMessageType().name().toLowerCase(Locale.ROOT), "content", message.getText());
    }
}
