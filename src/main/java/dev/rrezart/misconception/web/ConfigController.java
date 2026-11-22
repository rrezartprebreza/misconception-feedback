package dev.rrezart.misconception.web;

import dev.rrezart.misconception.model.ProgrammingLanguage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ProgrammingLanguage language;

    public ConfigController(ProgrammingLanguage language) {
        this.language = language;
    }

    @GetMapping
    public AppInfo info() {
        return new AppInfo(language.id(), language.displayName());
    }
}
