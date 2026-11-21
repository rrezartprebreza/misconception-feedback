package dev.rrezart.misconception.catalogue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fixed list of misconceptions the detector may report. Ids must be unique.
 */
public class MisconceptionCatalogue {

    private final Map<String, Misconception> byId = new LinkedHashMap<>();

    public MisconceptionCatalogue(List<Misconception> entries) {
        for (Misconception entry : entries) {
            if (byId.putIfAbsent(entry.id(), entry) != null) {
                throw new IllegalArgumentException("Duplicate misconception id: " + entry.id());
            }
        }
    }

    public static MisconceptionCatalogue fromClasspath(ObjectMapper mapper, String resource) {
        try (InputStream in = MisconceptionCatalogue.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("Catalogue resource not found: " + resource);
            }
            List<Misconception> entries = mapper.readValue(in, new TypeReference<List<Misconception>>() {
            });
            return new MisconceptionCatalogue(entries);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read catalogue " + resource, e);
        }
    }

    public List<Misconception> all() {
        return List.copyOf(byId.values());
    }

    public Optional<Misconception> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public int size() {
        return byId.size();
    }
}
