package dev.rrezart.misconception.language;

import dev.rrezart.misconception.model.ProgrammingLanguage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageFilesTest {

    @Test
    void addsTheLanguageBeforeTheExtension() {
        assertThat(LanguageFiles.withLanguage("study-data/submissions.jsonl", ProgrammingLanguage.PYTHON))
                .isEqualTo("study-data/submissions-python.jsonl");
        assertThat(LanguageFiles.withLanguage("log", ProgrammingLanguage.CSHARP)).isEqualTo("log-csharp");
        assertThat(LanguageFiles.withLanguage("a.b/log", ProgrammingLanguage.JAVA)).isEqualTo("a.b/log-java");
    }

    @Test
    void aCustomTasksFileAppliesOnlyToTheDefaultLanguageUnlessItHasAPlaceholder() {
        assertThat(LanguageFiles.tasksFile("my.json", ProgrammingLanguage.JAVA, ProgrammingLanguage.JAVA)).isEqualTo("my.json");
        assertThat(LanguageFiles.tasksFile("my.json", ProgrammingLanguage.PYTHON, ProgrammingLanguage.JAVA)).isNull();
        assertThat(LanguageFiles.tasksFile("tasks-{language}.json", ProgrammingLanguage.PYTHON, ProgrammingLanguage.JAVA))
                .isEqualTo("tasks-python.json");
        assertThat(LanguageFiles.tasksFile("", ProgrammingLanguage.JAVA, ProgrammingLanguage.JAVA)).isNull();
        assertThat(LanguageFiles.tasksFile(null, ProgrammingLanguage.JAVA, ProgrammingLanguage.JAVA)).isNull();
    }
}
