package dev.rrezart.misconception.feedback;

import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.detect.Detector;
import dev.rrezart.misconception.llm.LlmClient;
import dev.rrezart.misconception.model.FeedbackCondition;
import dev.rrezart.misconception.model.FeedbackResult;
import dev.rrezart.misconception.model.Match;
import dev.rrezart.misconception.model.Submission;

import java.util.List;

/**
 * Produces feedback for one submission under one study condition.
 * GENERIC never calls the detector, so the model cannot see its output.
 */
public class FeedbackService {

    private final LlmClient llm;
    private final Detector detector;
    private final MisconceptionCatalogue catalogue;
    private final PromptFactory prompts;
    private final Anonymiser anonymiser;

    public FeedbackService(LlmClient llm,
                           Detector detector,
                           MisconceptionCatalogue catalogue,
                           PromptFactory prompts,
                           Anonymiser anonymiser) {
        this.llm = llm;
        this.detector = detector;
        this.catalogue = catalogue;
        this.prompts = prompts;
        this.anonymiser = anonymiser;
    }

    public FeedbackResult feedback(Submission raw, FeedbackCondition condition) {
        Submission submission = anonymiser.anonymise(raw);
        List<Match> matches = condition == FeedbackCondition.INFORMED
                ? detector.detect(submission)
                : List.of();
        String text = llm.complete(
                prompts.feedbackSystem(),
                prompts.feedbackUser(submission, condition, matches, catalogue));
        return new FeedbackResult(condition, text, matches);
    }

    /**
     * Runs the detector only, for the RQ1 accuracy evaluation on the gold-labelled sample.
     */
    public List<Match> detectOnly(Submission raw) {
        return detector.detect(anonymiser.anonymise(raw));
    }
}
