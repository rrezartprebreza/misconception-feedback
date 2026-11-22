package dev.rrezart.misconception.web;

import dev.rrezart.misconception.language.LanguageRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final LanguageRegistry registry;

    public ConfigController(LanguageRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public AppInfo info() {
        return new AppInfo(
                registry.defaultLanguage().id(),
                registry.defaultLanguage().displayName(),
                registry.languages().stream()
                        .map(l -> new AppInfo.LanguageOption(l.id(), l.displayName()))
                        .toList());
    }
}
