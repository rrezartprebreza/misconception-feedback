package dev.rrezart.misconception.language;

import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.collect.CollectionService;
import dev.rrezart.misconception.feedback.FeedbackService;
import dev.rrezart.misconception.feedback.PromptFactory;
import dev.rrezart.misconception.model.ProgrammingLanguage;
import dev.rrezart.misconception.study.StudyService;
import dev.rrezart.misconception.study.StudyTasks;

/**
 * Everything that depends on the programming language: its catalogue, exercises, prompts and the
 * services built on them. One of these exists per supported language.
 */
public record LanguageContext(ProgrammingLanguage language,
                              MisconceptionCatalogue catalogue,
                              StudyTasks tasks,
                              PromptFactory prompts,
                              FeedbackService feedback,
                              StudyService study,
                              CollectionService collection) {
}
