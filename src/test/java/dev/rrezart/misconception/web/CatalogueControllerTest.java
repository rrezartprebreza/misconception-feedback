package dev.rrezart.misconception.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.language.LanguageContext;
import dev.rrezart.misconception.language.LanguageRegistry;
import dev.rrezart.misconception.model.ProgrammingLanguage;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogueControllerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private LanguageRegistry registry() {
        Map<ProgrammingLanguage, LanguageContext> contexts = new EnumMap<>(ProgrammingLanguage.class);
        for (ProgrammingLanguage language : ProgrammingLanguage.values()) {
            MisconceptionCatalogue catalogue = MisconceptionCatalogue.fromClasspath(mapper, language.catalogueResource());
            contexts.put(language, new LanguageContext(language, catalogue, null, null, null, null, null));
        }
        return new LanguageRegistry(contexts, ProgrammingLanguage.JAVA);
    }

    @Test
    void returnsTheCatalogueOfTheRequestedLanguage() {
        CatalogueController controller = new CatalogueController(registry());

        assertThat(controller.all("python").get(6).correction()).contains("RecursionError");
        assertThat(controller.all("csharp").get(7).correction()).contains("InvalidOperationException");
    }

    @Test
    void usesTheDefaultLanguageWhenNoneIsGiven() {
        CatalogueController controller = new CatalogueController(registry());

        assertThat(controller.all(null)).hasSize(8);
        assertThat(controller.all("")).isEqualTo(controller.all("java"));
    }

    @Test
    void anUnknownLanguageIsABadRequest() {
        CatalogueController controller = new CatalogueController(registry());

        assertThatThrownBy(() -> controller.all("rust")).isInstanceOf(ResponseStatusException.class);
    }
}
