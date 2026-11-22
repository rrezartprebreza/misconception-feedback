package dev.rrezart.misconception.language;

import dev.rrezart.misconception.model.ProgrammingLanguage;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LanguageRegistryTest {

    private LanguageRegistry registry(ProgrammingLanguage... available) {
        Map<ProgrammingLanguage, LanguageContext> contexts = new EnumMap<>(ProgrammingLanguage.class);
        for (ProgrammingLanguage language : available) {
            contexts.put(language, new LanguageContext(language, null, null, null, null, null, null));
        }
        return new LanguageRegistry(contexts, ProgrammingLanguage.PYTHON);
    }

    @Test
    void missingOrBlankIdMeansTheDefault() {
        LanguageRegistry registry = registry(ProgrammingLanguage.values());

        assertThat(registry.resolve(null).language()).isEqualTo(ProgrammingLanguage.PYTHON);
        assertThat(registry.resolve("  ").language()).isEqualTo(ProgrammingLanguage.PYTHON);
        assertThat(registry.defaultContext().language()).isEqualTo(ProgrammingLanguage.PYTHON);
    }

    @Test
    void resolvesByIdIgnoringCase() {
        LanguageRegistry registry = registry(ProgrammingLanguage.values());

        assertThat(registry.resolve("CSharp").language()).isEqualTo(ProgrammingLanguage.CSHARP);
        assertThat(registry.languages()).containsExactly(
                ProgrammingLanguage.JAVA, ProgrammingLanguage.PYTHON, ProgrammingLanguage.CSHARP);
    }

    @Test
    void unknownLanguageIsRejectedWithTheSupportedList() {
        LanguageRegistry registry = registry(ProgrammingLanguage.values());

        assertThatThrownBy(() -> registry.resolve("rust")).hasMessageContaining("java, python or csharp");
    }

    @Test
    void aLanguageWithoutAContextIsRejected() {
        LanguageRegistry registry = registry(ProgrammingLanguage.PYTHON);

        assertThatThrownBy(() -> registry.resolve("java")).hasMessageContaining("not available");
    }

    @Test
    void theDefaultLanguageMustHaveAContext() {
        assertThatThrownBy(() -> registry(ProgrammingLanguage.JAVA)).isInstanceOf(IllegalArgumentException.class);
    }
}
