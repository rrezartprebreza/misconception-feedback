package dev.rrezart.misconception.detect;

import dev.rrezart.misconception.FakeLlmClient;
import dev.rrezart.misconception.catalogue.Misconception;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.feedback.PromptFactory;
import dev.rrezart.misconception.model.DetectionResponse;
import dev.rrezart.misconception.model.Match;
import dev.rrezart.misconception.model.Submission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmDetectorTest {

    private final MisconceptionCatalogue catalogue = new MisconceptionCatalogue(List.of(
            new Misconception("M01", "a", "t", "b", "p", "c"),
            new Misconception("M03", "a", "t", "b", "p", "c")));
    private final Submission submission = new Submission("P01", "Task", "code", "why");

    private LlmDetector detectorReturning(DetectionResponse response, double threshold, FakeLlmClient llm) {
        llm.structuredResponse = response;
        return new LlmDetector(llm, catalogue, new PromptFactory(), threshold);
    }

    @Test
    void dropsLowConfidenceMatches() {
        FakeLlmClient llm = new FakeLlmClient();
        LlmDetector detector = detectorReturning(new DetectionResponse(List.of(
                new Match("M01", "e1", 0.9),
                new Match("M03", "e2", 0.3))), 0.6, llm);

        List<Match> matches = detector.detect(submission);

        assertThat(matches).extracting(Match::misconceptionId).containsExactly("M01");
    }

    @Test
    void dropsIdsThatAreNotInTheCatalogue() {
        FakeLlmClient llm = new FakeLlmClient();
        LlmDetector detector = detectorReturning(new DetectionResponse(List.of(
                new Match("M99", "invented", 0.95),
                new Match("M03", "real", 0.95))), 0.6, llm);

        assertThat(detector.detect(submission)).extracting(Match::misconceptionId).containsExactly("M03");
    }

    @Test
    void returnsEmptyListForNullResponse() {
        FakeLlmClient llm = new FakeLlmClient();
        LlmDetector detector = detectorReturning(null, 0.6, llm);

        assertThat(detector.detect(submission)).isEmpty();
    }

    @Test
    void sendsSubmissionAndCatalogueToTheModel() {
        FakeLlmClient llm = new FakeLlmClient();
        LlmDetector detector = detectorReturning(new DetectionResponse(List.of()), 0.6, llm);

        detector.detect(submission);

        assertThat(llm.lastSystem).contains("M01").contains("M03");
        assertThat(llm.lastUser).contains("code").contains("why");
    }
}
