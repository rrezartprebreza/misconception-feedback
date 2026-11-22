package dev.rrezart.misconception.catalogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MisconceptionCatalogueTest {

    @Test
    void loadsBundledCatalogue() {
        MisconceptionCatalogue catalogue = MisconceptionCatalogue.fromClasspath(new ObjectMapper(), "/catalogue-java.json");

        assertThat(catalogue.size()).isEqualTo(8);
        assertThat(catalogue.find("M03")).isPresent();
        assertThat(catalogue.find("M03").get().topic()).isEqualTo("Binary search");
        assertThat(catalogue.find("does-not-exist")).isEmpty();
    }

    @Test
    void everyEntryHasAllFields() {
        MisconceptionCatalogue catalogue = MisconceptionCatalogue.fromClasspath(new ObjectMapper(), "/catalogue-java.json");

        for (Misconception m : catalogue.all()) {
            assertThat(m.id()).isNotBlank();
            assertThat(m.name()).isNotBlank();
            assertThat(m.topic()).isNotBlank();
            assertThat(m.belief()).isNotBlank();
            assertThat(m.pattern()).isNotBlank();
            assertThat(m.correction()).isNotBlank();
        }
    }

    @Test
    void rejectsDuplicateIds() {
        Misconception a = new Misconception("X1", "a", "t", "b", "p", "c");
        Misconception b = new Misconception("X1", "b", "t", "b", "p", "c");

        assertThatThrownBy(() -> new MisconceptionCatalogue(List.of(a, b)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("X1");
    }
}
