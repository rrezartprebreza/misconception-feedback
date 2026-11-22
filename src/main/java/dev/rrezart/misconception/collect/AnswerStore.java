package dev.rrezart.misconception.collect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Answers and labels kept in two append-only JSON-lines files. Answers are never edited. A label
 * entry replaces the earlier one for the same answer and labeller, so labelling can be corrected.
 * Keep both files outside git: they hold student work.
 */
public class AnswerStore {

    private static final Pattern LABELLER = Pattern.compile("[A-Za-z0-9_-]{1,20}");

    private final Path answersFile;
    private final Path labelsFile;
    private final ObjectMapper mapper;
    private final List<CollectedAnswer> answers = new ArrayList<>();
    private final Map<String, Map<String, List<String>>> labels = new LinkedHashMap<>();

    public AnswerStore(Path answersFile, Path labelsFile, ObjectMapper mapper) {
        this.answersFile = answersFile;
        this.labelsFile = labelsFile;
        this.mapper = mapper;
        try {
            createParent(answersFile);
            createParent(labelsFile);
            for (String line : readLines(answersFile)) {
                answers.add(mapper.readValue(line, CollectedAnswer.class));
            }
            for (String line : readLines(labelsFile)) {
                LabelEntry entry = mapper.readValue(line, new TypeReference<LabelEntry>() {
                });
                labels.computeIfAbsent(entry.labeller(), k -> new LinkedHashMap<>()).put(entry.id(), entry.labels());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not open the answer store", e);
        }
    }

    public static boolean isValidLabeller(String name) {
        return name != null && LABELLER.matcher(name).matches();
    }

    public synchronized CollectedAnswer add(String collectedAt, String participantId, String taskId, String task,
                                            String language, String code, String explanation) {
        String id = String.format("A%04d", answers.size() + 1);
        CollectedAnswer answer = new CollectedAnswer(id, collectedAt, participantId, taskId, task, language, code, explanation);
        append(answersFile, answer);
        answers.add(answer);
        return answer;
    }

    public synchronized List<CollectedAnswer> all() {
        return List.copyOf(answers);
    }

    public synchronized Optional<CollectedAnswer> find(String id) {
        return answers.stream().filter(a -> a.id().equals(id)).findFirst();
    }

    public synchronized void setLabels(String labeller, String answerId, List<String> ids, String at) {
        if (!isValidLabeller(labeller)) {
            throw new IllegalArgumentException("Labeller name must be 1 to 20 letters, digits, - or _");
        }
        if (find(answerId).isEmpty()) {
            throw new IllegalArgumentException("Unknown answer: " + answerId);
        }
        List<String> copy = List.copyOf(ids);
        append(labelsFile, new LabelEntry(answerId, labeller, copy, at));
        labels.computeIfAbsent(labeller, k -> new LinkedHashMap<>()).put(answerId, copy);
    }

    /** Empty when this labeller has not labelled the answer yet. */
    public synchronized Optional<List<String>> labelsOf(String labeller, String answerId) {
        return Optional.ofNullable(labels.getOrDefault(labeller, Map.of()).get(answerId));
    }

    public synchronized List<String> labellers() {
        return List.copyOf(labels.keySet());
    }

    private void append(Path file, Object value) {
        try {
            Files.writeString(file, mapper.writeValueAsString(value) + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + file, e);
        }
    }

    private static List<String> readLines(Path file) throws IOException {
        if (!Files.exists(file)) {
            return List.of();
        }
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream().filter(l -> !l.isBlank()).toList();
    }

    private static void createParent(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
