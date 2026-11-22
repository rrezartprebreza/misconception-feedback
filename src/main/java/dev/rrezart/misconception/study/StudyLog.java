package dev.rrezart.misconception.study;

/**
 * Where study submissions are recorded.
 */
public interface StudyLog {

    /** How many submissions this participant has already made for this task. */
    int countAttempts(String participantId, String taskId);

    void append(StudyRecord record);
}
