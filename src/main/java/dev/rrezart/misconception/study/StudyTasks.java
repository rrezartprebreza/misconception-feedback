package dev.rrezart.misconception.study;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The exercises available in the study. Ids must be unique and every topic must be 1 or 2.
 */
public class StudyTasks {

    private final Map<String, StudyTask> byId = new LinkedHashMap<>();

    public StudyTasks(List<StudyTask> tasks) {
        for (StudyTask task : tasks) {
            if (task.topic() != 1 && task.topic() != 2) {
                throw new IllegalArgumentException("Task " + task.id() + " must have topic 1 or 2");
            }
            if (byId.putIfAbsent(task.id(), task) != null) {
                throw new IllegalArgumentException("Duplicate task id: " + task.id());
            }
        }
    }

    public static StudyTasks fromClasspath(ObjectMapper mapper, String resource) {
        try (InputStream in = StudyTasks.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("Task resource not found: " + resource);
            }
            return new StudyTasks(mapper.readValue(in, new TypeReference<List<StudyTask>>() {
            }));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read tasks " + resource, e);
        }
    }

    public static StudyTasks fromFile(ObjectMapper mapper, Path file) {
        try {
            return new StudyTasks(mapper.readValue(Files.readString(file), new TypeReference<List<StudyTask>>() {
            }));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read tasks " + file, e);
        }
    }

    public List<StudyTask> all() {
        return List.copyOf(byId.values());
    }

    public Optional<StudyTask> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }
}
