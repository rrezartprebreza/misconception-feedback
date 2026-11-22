package dev.rrezart.misconception.study;

/**
 * An exercise given to participants. Topic 1 or 2 decides which feedback type a participant gets.
 */
public record StudyTask(String id, int topic, String title, String text) {
}
