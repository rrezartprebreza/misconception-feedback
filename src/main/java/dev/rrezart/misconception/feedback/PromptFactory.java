package dev.rrezart.misconception.feedback;

import dev.rrezart.misconception.catalogue.Misconception;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.model.FeedbackCondition;
import dev.rrezart.misconception.model.Match;
import dev.rrezart.misconception.model.Submission;

import java.util.List;

/**
 * Builds every prompt used in the study. Kept free of framework code so the exact wording can be
 * reviewed, tested and published with the paper.
 */
public class PromptFactory {

    public String detectionSystem(MisconceptionCatalogue catalogue) {
        StringBuilder sb = new StringBuilder();
        sb.append("You analyse a student's Java answer for a data structures and algorithms course.\n");
        sb.append("Decide which misconceptions from the catalogue below the answer shows evidence of.\n");
        sb.append("Rules: use only ids from the catalogue; point to the evidence in the answer; ");
        sb.append("give a confidence between 0 and 1; return an empty list if there is no evidence.\n\n");
        sb.append("Catalogue:\n");
        for (Misconception m : catalogue.all()) {
            sb.append("- ").append(m.id()).append(" (").append(m.topic()).append("): ")
                    .append(m.belief()).append(" Typical pattern: ").append(m.pattern()).append('\n');
        }
        return sb.toString();
    }

    public String detectionUser(Submission submission) {
        return submissionBlock(submission);
    }

    public String feedbackSystem() {
        return "You give short feedback to a student on a Java data structures and algorithms exercise. "
                + "Do not give the full solution. Explain what is wrong or unclear, and end with one "
                + "guiding question that helps the student fix it themselves. Keep it under 150 words.";
    }

    /**
     * GENERIC: task, code and explanation only. INFORMED: the same, plus each detected misconception
     * with its false belief and corrective explanation. If nothing was detected, INFORMED is identical
     * to GENERIC, so the share of such cases should be reported in the paper.
     */
    public String feedbackUser(Submission submission,
                               FeedbackCondition condition,
                               List<Match> matches,
                               MisconceptionCatalogue catalogue) {
        StringBuilder sb = new StringBuilder(submissionBlock(submission));
        if (condition == FeedbackCondition.INFORMED && matches != null && !matches.isEmpty()) {
            sb.append("\n\nAn analysis found that this answer may reflect the following misconception(s). ");
            sb.append("Address the underlying belief directly, not only the symptom.\n");
            for (Match match : matches) {
                catalogue.find(match.misconceptionId()).ifPresent(m ->
                        sb.append("- ").append(m.name()).append(": the student may believe that ")
                                .append(lowerFirst(m.belief())).append(" Correct idea: ")
                                .append(m.correction()).append('\n'));
            }
        }
        return sb.toString();
    }

    private String submissionBlock(Submission s) {
        String explanation = (s.explanation() == null || s.explanation().isBlank()) ? "(none)" : s.explanation();
        return "Task:\n" + s.task()
                + "\n\nStudent code:\n```java\n" + s.code() + "\n```"
                + "\n\nStudent explanation:\n" + explanation;
    }

    private String lowerFirst(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }
}
