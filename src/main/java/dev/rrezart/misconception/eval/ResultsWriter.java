package dev.rrezart.misconception.eval;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Saves one pilot run so it can go into the paper: a JSON file with the scores and every item's
 * prediction, and a text file with the same summary the console shows. File names carry the
 * language, the model and the time, so runs for different models never overwrite each other.
 * Item ids are written, never the student code.
 */
public final class ResultsWriter {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private ResultsWriter() {
    }

    /** Returns the path of the JSON file. The text summary sits next to it with the extension .txt. */
    public static Path write(ObjectMapper mapper, Path directory, String language, String model, Instant when,
                             PilotEvaluator.Detailed detailed) throws IOException {
        Files.createDirectories(directory);
        String base = language + "-" + safe(model) + "-" + STAMP.format(when);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("language", language);
        out.put("model", model);
        out.put("createdAt", when.toString());
        out.put("items", detailed.report().items());
        out.put("micro", detailed.report().micro());
        out.put("kappaById", detailed.report().kappaById());
        out.put("perMisconception", detailed.perMisconception());
        out.put("results", detailed.items());
        Path json = directory.resolve(base + ".json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(json.toFile(), out);
        Files.writeString(directory.resolve(base + ".txt"), PilotEvaluator.formatDetailed(detailed));
        return json;
    }

    static String safe(String model) {
        String cleaned = (model == null || model.isBlank() ? "unspecified" : model).replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.length() > 60 ? cleaned.substring(0, 60) : cleaned;
    }
}
