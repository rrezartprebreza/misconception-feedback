package dev.rrezart.misconception.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rrezart.misconception.collect.AnswerStore;
import dev.rrezart.misconception.collect.ParticipantIds;
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

    /** Empty (the default) accepts any valid ID. Set COLLECT_PARTICIPANT_IDS, for example S01..S60, to allow only those. */
    @Bean
    ParticipantIds participantIds(@Value("${collect.participant-ids:}") String spec) {
        return ParticipantIds.parse(spec);
    }
}
