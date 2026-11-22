package dev.rrezart.misconception.model;

/**
 * Languages the tool supports. Each has its own catalogue, study exercises and synthetic pilot set.
 */
public enum ProgrammingLanguage {
    JAVA("java", "Java", "java"),
    PYTHON("python", "Python", "python"),
    CSHARP("csharp", "C#", "csharp");

    private final String id;
    private final String displayName;
    private final String fence;

    ProgrammingLanguage(String id, String displayName, String fence) {
        this.id = id;
        this.displayName = displayName;
        this.fence = fence;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** Tag used after the opening code fence in prompts. */
    public String fence() {
        return fence;
    }

    public String catalogueResource() {
        return "/catalogue-" + id + ".json";
    }

    public String tasksResource() {
        return "/study/tasks-" + id + ".json";
    }

    public String goldResource() {
        return "/pilot/synthetic-gold-" + id + ".json";
    }

    public static ProgrammingLanguage fromId(String text) {
        for (ProgrammingLanguage language : values()) {
            if (language.id.equalsIgnoreCase(text)) {
                return language;
            }
        }
        throw new IllegalArgumentException("Unknown language '" + text + "'. Use java, python or csharp.");
    }
}
