package dev.rrezart.misconception.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.eval.GoldItem;
import dev.rrezart.misconception.study.StudyTasks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LanguageResourcesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @EnumSource(ProgrammingLanguage.class)
    void everyLanguageHasACatalogueWithTheSameIds(ProgrammingLanguage language) {
        MisconceptionCatalogue reference = MisconceptionCatalogue.fromClasspath(mapper,
                ProgrammingLanguage.JAVA.catalogueResource());
        MisconceptionCatalogue catalogue = MisconceptionCatalogue.fromClasspath(mapper, language.catalogueResource());

        assertThat(catalogue.all().stream().map(m -> m.id()).toList())
                .isEqualTo(reference.all().stream().map(m -> m.id()).toList());
    }

    @ParameterizedTest
    @EnumSource(ProgrammingLanguage.class)
    void everyLanguageHasTheSameStudyTasks(ProgrammingLanguage language) {
        StudyTasks reference = StudyTasks.fromClasspath(mapper, ProgrammingLanguage.JAVA.tasksResource());
        StudyTasks tasks = StudyTasks.fromClasspath(mapper, language.tasksResource());

        assertThat(tasks.all().stream().map(t -> t.id() + ":" + t.topic()).toList())
                .isEqualTo(reference.all().stream().map(t -> t.id() + ":" + t.topic()).toList());
    }

    @ParameterizedTest
    @EnumSource(ProgrammingLanguage.class)
    void everyGoldSetUsesOnlyItsCatalogueIds(ProgrammingLanguage language) throws IOException {
        MisconceptionCatalogue catalogue = MisconceptionCatalogue.fromClasspath(mapper, language.catalogueResource());
        try (InputStream in = getClass().getResourceAsStream(language.goldResource())) {
            assertThat(in).as(language.goldResource()).isNotNull();
            List<GoldItem> items = mapper.readValue(in, new TypeReference<List<GoldItem>>() {
            });
            assertThat(items).hasSize(12);
            for (GoldItem item : items) {
                for (String label : item.labels()) {
                    assertThat(catalogue.find(label)).as(item.id() + " " + label).isPresent();
                }
            }
        }
    }

    @Test
    void fromIdIsCaseInsensitiveAndRejectsUnknownLanguages() {
        assertThat(ProgrammingLanguage.fromId("Python")).isEqualTo(ProgrammingLanguage.PYTHON);
        assertThat(ProgrammingLanguage.fromId("CSHARP")).isEqualTo(ProgrammingLanguage.CSHARP);
        assertThatThrownBy(() -> ProgrammingLanguage.fromId("rust"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("java, python or csharp");
    }
}
