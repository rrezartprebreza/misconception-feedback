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
import dev.rrezart.misconception.model.ProgrammingLanguage;
import dev.rrezart.misconception.llm.FallbackLlmClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class AppConfig {

    /** One language per running instance: java, python or csharp. */
    @Bean
    ProgrammingLanguage programmingLanguage(@Value("${study.language:java}") String id) {
        return ProgrammingLanguage.fromId(id);
    }

    @Bean
    MisconceptionCatalogue catalogue(ObjectMapper mapper, ProgrammingLanguage language) {
        return MisconceptionCatalogue.fromClasspath(mapper, language.catalogueResource());
    }

    @Bean
    PromptFactory promptFactory(ProgrammingLanguage language) {
        return new PromptFactory(language);
    }

    @Bean
    Anonymiser anonymiser() {
        return new Anonymiser();
    }

    @Bean
    LlmClient llmClient(ChatClient.Builder builder,
                        @Value("${llm.fallback-models:}") String fallbackModels,
                        @Value("${llm.fallback-cooldown-seconds:60}") long cooldownSeconds) {
        List<String> fallbacks = Arrays.stream(fallbackModels.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
        if (fallbacks.isEmpty()) {
            return new SpringAiLlmClient(builder);
        }
        List<LlmClient> chain = new ArrayList<>();
        chain.add(new SpringAiLlmClient(builder));
        fallbacks.forEach(model -> chain.add(new SpringAiLlmClient(builder, model)));
        return new FallbackLlmClient(chain, Duration.ofSeconds(cooldownSeconds), Clock.systemUTC());
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
