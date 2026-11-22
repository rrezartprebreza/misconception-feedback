package dev.rrezart.misconception.web;

import dev.rrezart.misconception.language.LanguageContext;
import dev.rrezart.misconception.language.LanguageRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Turns an unknown language id into a 400 response. */
final class Languages {

    private Languages() {
    }

    static LanguageContext resolve(LanguageRegistry registry, String id) {
        try {
            return registry.resolve(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
