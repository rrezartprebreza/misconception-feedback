package dev.rrezart.misconception.study;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Appends one JSON object per line to a file. Existing lines are read at start-up so attempt
 * numbers continue correctly after a restart. Keep the file outside git: it holds student work.
 */
public class JsonlStudyLog implements StudyLog {

    private final Path file;
    private final ObjectMapper mapper;
    private final Map<String, Integer> counts = new HashMap<>();

    public JsonlStudyLog(Path file, ObjectMapper mapper) {
        this.file = file;
        this.mapper = mapper;
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (line.isBlank()) {
                        continue;
                    }
                    JsonNode node = mapper.readTree(line);
                    counts.merge(key(node.path("participantId").asText(), node.path("taskId").asText()), 1, Integer::sum);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not open study log " + file, e);
        }
    }

    @Override
    public synchronized int countAttempts(String participantId, String taskId) {
        return counts.getOrDefault(key(participantId, taskId), 0);
    }

    @Override
    public synchronized void append(StudyRecord record) {
        try {
            String line = mapper.writeValueAsString(record) + System.lineSeparator();
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            counts.merge(key(record.participantId(), record.taskId()), 1, Integer::sum);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write study log " + file, e);
        }
    }

    private static String key(String participantId, String taskId) {
        return participantId + "|" + taskId;
    }
}
