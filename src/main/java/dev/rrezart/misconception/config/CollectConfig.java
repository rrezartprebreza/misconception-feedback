package dev.rrezart.misconception.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.collect.AnswerStore;
import dev.rrezart.misconception.collect.CollectionService;
import dev.rrezart.misconception.feedback.Anonymiser;
import dev.rrezart.misconception.model.ProgrammingLanguage;
import dev.rrezart.misconception.study.StudyTasks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.time.Clock;

@Configuration
public class CollectConfig {

    @Bean
    AnswerStore answerStore(ObjectMapper mapper,
                            @Value("${collect.answers-file:study-data/answers.jsonl}") String answersFile,
                            @Value("${collect.labels-file:study-data/labels.jsonl}") String labelsFile) {
        return new AnswerStore(Path.of(answersFile), Path.of(labelsFile), mapper);
    }

    @Bean
    CollectionService collectionService(AnswerStore store,
                                        StudyTasks tasks,
                                        MisconceptionCatalogue catalogue,
                                        Anonymiser anonymiser,
                                        ProgrammingLanguage language) {
        return new CollectionService(store, tasks, catalogue, anonymiser, language, Clock.systemUTC());
    }
}
