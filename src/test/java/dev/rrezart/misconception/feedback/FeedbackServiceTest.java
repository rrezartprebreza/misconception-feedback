package dev.rrezart.misconception.feedback;

import dev.rrezart.misconception.FakeLlmClient;
import dev.rrezart.misconception.catalogue.Misconception;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.detect.Detector;
import dev.rrezart.misconception.model.FeedbackCondition;
import dev.rrezart.misconception.model.FeedbackResult;
import dev.rrezart.misconception.model.Match;
import dev.rrezart.misconception.model.Submission;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackServiceTest {

    private final MisconceptionCatalogue catalogue = new MisconceptionCatalogue(List.of(
            new Misconception("M03", "Search bounds", "Binary search",
                    "Any update works.", "hi = mid", "Make each iteration shrink the interval.")));
    private final Submission submission = new Submission("P01", "Task", "while (lo < hi) { hi = mid; }", "why");
    private final AtomicInteger detectorCalls = new AtomicInteger();
    private final Detector detector = s -> {
        detectorCalls.incrementAndGet();
        return List.of(new Match("M03", "hi = mid", 0.9));
    };
    private final FakeLlmClient llm = new FakeLlmClient();
    private final FeedbackService service =
            new FeedbackService(llm, detector, catalogue, new PromptFactory(), new Anonymiser());

    @Test
    void genericConditionNeverCallsTheDetector() {
        FeedbackResult result = service.feedback(submission, FeedbackCondition.GENERIC);

        assertThat(detectorCalls.get()).isZero();
        assertThat(result.matches()).isEmpty();
        assertThat(result.feedback()).isEqualTo("FEEDBACK");
        assertThat(llm.lastUser).doesNotContain("Search bounds");
    }

    @Test
    void informedConditionPutsTheMisconceptionInThePrompt() {
        FeedbackResult result = service.feedback(submission, FeedbackCondition.INFORMED);

        assertThat(detectorCalls.get()).isEqualTo(1);
        assertThat(result.matches()).extracting(Match::misconceptionId).containsExactly("M03");
        assertThat(llm.lastUser).contains("Search bounds").contains("shrink the interval");
    }

    @Test
    void submissionIsAnonymisedBeforeItReachesTheModel() {
        Submission withEmail = new Submission("P02", "Task", "int x; // a.b@example.com", "why");

        service.feedback(withEmail, FeedbackCondition.GENERIC);

        assertThat(llm.lastUser).doesNotContain("a.b@example.com");
    }

    @Test
    void detectOnlyRunsTheDetectorWithoutCallingTheFeedbackModel() {
        List<Match> matches = service.detectOnly(submission);

        assertThat(matches).hasSize(1);
        assertThat(llm.completeCalls).isZero();
    }
}
