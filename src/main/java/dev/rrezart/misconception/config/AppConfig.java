package dev.rrezart.misconception.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.detect.Detector;
import dev.rrezart.misconception.detect.LlmDetector;
import dev.rrezart.misconception.feedback.Anonymiser;
import dev.rrezart.misconception.feedback.FeedbackService;
import dev.rrezart.misconception.feedback.PromptFactory;
import dev.rrezart.misconception.llm.LlmClient;
import dev.rrezart.misconception.llm.SpringAiLlmClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    MisconceptionCatalogue catalogue(ObjectMapper mapper) {
        return MisconceptionCatalogue.fromClasspath(mapper, "/catalogue.json");
    }

    @Bean
    PromptFactory promptFactory() {
        return new PromptFactory();
    }

    @Bean
    Anonymiser anonymiser() {
        return new Anonymiser();
    }

    @Bean
    LlmClient llmClient(ChatClient.Builder builder) {
        return new SpringAiLlmClient(builder);
    }

    @Bean
    Detector detector(LlmClient llm,
                      MisconceptionCatalogue catalogue,
                      PromptFactory prompts,
                      @Value("${study.detector.threshold:0.6}") double threshold) {
        return new LlmDetector(llm, catalogue, prompts, threshold);
    }

    @Bean
    FeedbackService feedbackService(LlmClient llm,
                                    Detector detector,
                                    MisconceptionCatalogue catalogue,
                                    PromptFactory prompts,
                                    Anonymiser anonymiser) {
        return new FeedbackService(llm, detector, catalogue, prompts, anonymiser);
    }
}
