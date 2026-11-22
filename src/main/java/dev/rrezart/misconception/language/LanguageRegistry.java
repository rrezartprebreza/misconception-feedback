package dev.rrezart.misconception.language;

import dev.rrezart.misconception.model.ProgrammingLanguage;

import java.util.List;
import java.util.Map;

/**
 * Finds the context for a language id. A missing id means the instance's default language; an unknown
 * id is an error with a message listing the supported languages.
 */
public class LanguageRegistry {

    private final Map<ProgrammingLanguage, LanguageContext> contexts;
    private final ProgrammingLanguage defaultLanguage;

    public LanguageRegistry(Map<ProgrammingLanguage, LanguageContext> contexts, ProgrammingLanguage defaultLanguage) {
        if (!contexts.containsKey(defaultLanguage)) {
            throw new IllegalArgumentException("No context for the default language " + defaultLanguage.id());
        }
        this.contexts = Map.copyOf(contexts);
        this.defaultLanguage = defaultLanguage;
    }

    public LanguageContext resolve(String id) {
        if (id == null || id.isBlank()) {
            return defaultContext();
        }
        ProgrammingLanguage language = ProgrammingLanguage.fromId(id.trim());
        LanguageContext context = contexts.get(language);
        if (context == null) {
            throw new IllegalArgumentException("Language not available: " + id);
        }
        return context;
    }

    public LanguageContext defaultContext() {
        return contexts.get(defaultLanguage);
    }

    public ProgrammingLanguage defaultLanguage() {
        return defaultLanguage;
    }

    /** Supported languages in declaration order. */
    public List<ProgrammingLanguage> languages() {
        return List.of(ProgrammingLanguage.values()).stream().filter(contexts::containsKey).toList();
    }
}
