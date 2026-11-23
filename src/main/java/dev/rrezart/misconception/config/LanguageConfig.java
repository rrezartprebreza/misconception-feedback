package dev.rrezart.misconception.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.collect.AnswerStore;
import dev.rrezart.misconception.collect.CollectionService;
import dev.rrezart.misconception.collect.ParticipantIds;
import dev.rrezart.misconception.detect.Detector;
import dev.rrezart.misconception.detect.LlmDetector;
import dev.rrezart.misconception.feedback.Anonymiser;
import dev.rrezart.misconception.feedback.FeedbackService;
import dev.rrezart.misconception.feedback.PromptFactory;
import dev.rrezart.misconception.language.LanguageContext;
import dev.rrezart.misconception.language.LanguageFiles;
import dev.rrezart.misconception.language.LanguageRegistry;
import dev.rrezart.misconception.llm.LlmClient;
import dev.rrezart.misconception.model.ProgrammingLanguage;
import dev.rrezart.misconception.study.JsonlStudyLog;
import dev.rrezart.misconception.study.StudyLog;
import dev.rrezart.misconception.study.StudyService;
import dev.rrezart.misconception.study.StudyTasks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;

/**
 * Builds one context per supported language so a single running instance can serve Java, Python and C#.
 */
@Configuration
public class LanguageConfig {

    @Bean
    ModelLabel modelLabel(@Value("${study.model-label:auto}") String configured,
                          @Value("${spring.ai.openai.chat.options.model:}") String openAiModel,
                          @Value("${spring.ai.ollama.chat.options.model:}") String ollamaModel) {
        return ModelLabel.resolve(configured, ModelLabel.openAiOnClasspath(), openAiModel, ollamaModel);
    }

    @Bean
    LanguageRegistry languageRegistry(ObjectMapper mapper,
                                      LlmClient llm,
                                      Anonymiser anonymiser,
                                      AnswerStore answers,
                                      ParticipantIds participants,
                                      ProgrammingLanguage defaultLanguage,
                                      @Value("${study.detector.threshold:0.6}") double threshold,
                                      @Value("${study.tasks-file:}") String tasksFile,
                                      @Value("${study.log-file:study-data/submissions.jsonl}") String logFile,
                                      ModelLabel modelLabel) {
        Map<ProgrammingLanguage, LanguageContext> contexts = new EnumMap<>(ProgrammingLanguage.class);
        for (ProgrammingLanguage language : ProgrammingLanguage.values()) {
            MisconceptionCatalogue catalogue = MisconceptionCatalogue.fromClasspath(mapper, language.catalogueResource());
            PromptFactory prompts = new PromptFactory(language);
            Detector detector = new LlmDetector(llm, catalogue, prompts, threshold);
            FeedbackService feedback = new FeedbackService(llm, detector, catalogue, prompts, anonymiser);

            String customTasks = LanguageFiles.tasksFile(tasksFile, language, defaultLanguage);
            StudyTasks tasks = customTasks != null
                    ? StudyTasks.fromFile(mapper, Path.of(customTasks))
                    : StudyTasks.fromClasspath(mapper, language.tasksResource());

            StudyLog log = new JsonlStudyLog(Path.of(LanguageFiles.withLanguage(logFile, language)), mapper);
            StudyService study = new StudyService(feedback, tasks, log, anonymiser, Clock.systemUTC(), modelLabel.value());
            CollectionService collection = new CollectionService(answers, tasks, catalogue, anonymiser, language, Clock.systemUTC(), participants);

            contexts.put(language, new LanguageContext(language, catalogue, tasks, prompts, feedback, study, collection));
        }
        return new LanguageRegistry(contexts, defaultLanguage);
    }
}
