package dev.rrezart.misconception.language;

import dev.rrezart.misconception.model.ProgrammingLanguage;

/** File-name rules for per-language data files. */
public final class LanguageFiles {

    private LanguageFiles() {
    }

    /** study-data/submissions.jsonl becomes study-data/submissions-python.jsonl. */
    public static String withLanguage(String path, ProgrammingLanguage language) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        int dot = path.lastIndexOf('.');
        if (dot > slash) {
            return path.substring(0, dot) + "-" + language.id() + path.substring(dot);
        }
        return path + "-" + language.id();
    }

    /**
     * A tasks file with {language} in its name is used per language. Without the placeholder the file
     * applies only to the default language, because exercise text names its language.
     * Returns null when the bundled exercises should be used.
     */
    public static String tasksFile(String configured, ProgrammingLanguage language, ProgrammingLanguage defaultLanguage) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        if (configured.contains("{language}")) {
            return configured.replace("{language}", language.id());
        }
        return language == defaultLanguage ? configured : null;
    }
}
