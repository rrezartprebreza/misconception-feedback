package dev.rrezart.misconception.web;

import dev.rrezart.misconception.catalogue.Misconception;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes the catalogue so the web page can show a misconception's name and corrective idea.
 */
@RestController
@RequestMapping("/api/catalogue")
public class CatalogueController {

    private final MisconceptionCatalogue catalogue;

    public CatalogueController(MisconceptionCatalogue catalogue) {
        this.catalogue = catalogue;
    }

    @GetMapping
    public List<Misconception> all() {
        return catalogue.all();
    }
}
