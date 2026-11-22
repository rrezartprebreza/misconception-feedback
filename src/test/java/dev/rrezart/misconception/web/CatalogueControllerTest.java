package dev.rrezart.misconception.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogueControllerTest {

    @Test
    void returnsEveryCatalogueEntry() {
        MisconceptionCatalogue catalogue = MisconceptionCatalogue.fromClasspath(new ObjectMapper(), "/catalogue-java.json");

        assertThat(new CatalogueController(catalogue).all()).hasSize(catalogue.size());
    }
}
