package dev.rrezart.misconception.detect;

import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.feedback.PromptFactory;
import dev.rrezart.misconception.llm.LlmClient;
import dev.rrezart.misconception.model.DetectionResponse;
import dev.rrezart.misconception.model.Match;
import dev.rrezart.misconception.model.Submission;

import java.util.List;

/**
 * Asks the model which catalogue misconceptions a submission shows, then drops matches below the
 * confidence threshold and any id that is not in the catalogue (guards against invented ids).
 */
public class LlmDetector implements Detector {

    private final LlmClient llm;
    private final MisconceptionCatalogue catalogue;
    private final PromptFactory prompts;
    private final double threshold;

    public LlmDetector(LlmClient llm, MisconceptionCatalogue catalogue, PromptFactory prompts, double threshold) {
        this.llm = llm;
        this.catalogue = catalogue;
        this.prompts = prompts;
        this.threshold = threshold;
    }

    @Override
    public List<Match> detect(Submission submission) {
        DetectionResponse response = llm.completeStructured(
                prompts.detectionSystem(catalogue),
                prompts.detectionUser(submission),
                DetectionResponse.class);
        if (response == null || response.matches() == null) {
            return List.of();
        }
        return response.matches().stream()
                .filter(m -> m.confidence() >= threshold)
                .filter(m -> catalogue.find(m.misconceptionId()).isPresent())
                .toList();
    }
}
