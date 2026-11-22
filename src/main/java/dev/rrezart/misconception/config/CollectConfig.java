package dev.rrezart.misconception.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rrezart.misconception.collect.AnswerStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class CollectConfig {

    @Bean
    AnswerStore answerStore(ObjectMapper mapper,
                            @Value("${collect.answers-file:study-data/answers.jsonl}") String answersFile,
                            @Value("${collect.labels-file:study-data/labels.jsonl}") String labelsFile) {
        return new AnswerStore(Path.of(answersFile), Path.of(labelsFile), mapper);
    }
}
