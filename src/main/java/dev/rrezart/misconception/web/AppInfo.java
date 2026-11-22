package dev.rrezart.misconception.web;

import java.util.List;

/**
 * What the web pages need to know about this instance: the default language and every language
 * the page can switch to.
 */
public record AppInfo(String languageId, String languageName, List<LanguageOption> languages) {

    public record LanguageOption(String id, String name) {
    }
}
