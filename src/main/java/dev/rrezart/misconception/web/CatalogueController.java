package dev.rrezart.misconception.web;

import dev.rrezart.misconception.catalogue.Misconception;
import dev.rrezart.misconception.language.LanguageRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes the catalogue so the web page can show a misconception's name and corrective idea.
 * An optional language parameter picks the language; without it the default language is used.
 */
@RestController
@RequestMapping("/api/catalogue")
public class CatalogueController {

    private final LanguageRegistry registry;

    public CatalogueController(LanguageRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<Misconception> all(@RequestParam(required = false) String language) {
        return Languages.resolve(registry, language).catalogue().all();
    }
}
