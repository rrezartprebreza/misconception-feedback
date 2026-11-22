package dev.rrezart.misconception.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rrezart.misconception.feedback.Anonymiser;
import dev.rrezart.misconception.feedback.FeedbackService;
import dev.rrezart.misconception.study.JsonlStudyLog;
import dev.rrezart.misconception.study.StudyLog;
import dev.rrezart.misconception.study.StudyService;
import dev.rrezart.misconception.study.StudyTasks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.time.Clock;

@Configuration
public class StudyConfig {

    @Bean
    StudyTasks studyTasks(ObjectMapper mapper, @Value("${study.tasks-file:}") String tasksFile) {
        if (tasksFile != null && !tasksFile.isBlank()) {
            return StudyTasks.fromFile(mapper, Path.of(tasksFile));
        }
        return StudyTasks.fromClasspath(mapper, "/study/tasks.json");
    }

    @Bean
    StudyLog studyLog(ObjectMapper mapper, @Value("${study.log-file:study-data/submissions.jsonl}") String logFile) {
        return new JsonlStudyLog(Path.of(logFile), mapper);
    }

    @Bean
    StudyService studyService(FeedbackService feedback,
                              StudyTasks tasks,
                              StudyLog log,
                              Anonymiser anonymiser,
                              @Value("${study.model-label:unspecified}") String modelLabel) {
        return new StudyService(feedback, tasks, log, anonymiser, Clock.systemUTC(), modelLabel);
    }
}
