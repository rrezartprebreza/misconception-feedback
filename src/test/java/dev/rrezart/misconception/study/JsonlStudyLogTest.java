package dev.rrezart.misconception.study;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rrezart.misconception.model.FeedbackCondition;
import dev.rrezart.misconception.model.Match;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonlStudyLogTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private StudyRecord record(String participant, String task, int attempt) {
        return new StudyRecord("2026-11-22T10:00:00Z", participant, task, 1, attempt,
                FeedbackCondition.INFORMED, "model-x", "int x;\nint y;", "why", "feedback text",
                List.of(new Match("M03", "hi = mid", 0.9)));
    }

    @Test
    void writesOneJsonObjectPerLine(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("nested/log.jsonl");
        JsonlStudyLog log = new JsonlStudyLog(file, mapper);

        log.append(record("P01", "binary-search", 1));
        log.append(record("P01", "binary-search", 2));

        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(2);
        JsonNode first = mapper.readTree(lines.get(0));
        assertThat(first.get("participantId").asText()).isEqualTo("P01");
        assertThat(first.get("code").asText()).isEqualTo("int x;\nint y;");
        assertThat(first.get("condition").asText()).isEqualTo("INFORMED");
        assertThat(first.get("matches").get(0).get("misconceptionId").asText()).isEqualTo("M03");
    }

    @Test
    void countsAttemptsPerParticipantAndTask(@TempDir Path dir) {
        JsonlStudyLog log = new JsonlStudyLog(dir.resolve("log.jsonl"), mapper);

        log.append(record("P01", "binary-search", 1));
        log.append(record("P01", "binary-search", 2));
        log.append(record("P01", "list-insert", 1));

        assertThat(log.countAttempts("P01", "binary-search")).isEqualTo(2);
        assertThat(log.countAttempts("P01", "list-insert")).isEqualTo(1);
        assertThat(log.countAttempts("P02", "binary-search")).isZero();
    }

    @Test
    void attemptCountsSurviveARestart(@TempDir Path dir) {
        Path file = dir.resolve("log.jsonl");
        JsonlStudyLog first = new JsonlStudyLog(file, mapper);
        first.append(record("P01", "binary-search", 1));

        JsonlStudyLog second = new JsonlStudyLog(file, mapper);

        assertThat(second.countAttempts("P01", "binary-search")).isEqualTo(1);
    }
}
