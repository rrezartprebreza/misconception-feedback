package dev.rrezart.misconception.study;

import dev.rrezart.misconception.FakeLlmClient;
import dev.rrezart.misconception.catalogue.Misconception;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.feedback.Anonymiser;
import dev.rrezart.misconception.feedback.FeedbackService;
import dev.rrezart.misconception.feedback.PromptFactory;
import dev.rrezart.misconception.model.FeedbackCondition;
import dev.rrezart.misconception.model.Match;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudyServiceTest {

    private final MisconceptionCatalogue catalogue = new MisconceptionCatalogue(List.of(
            new Misconception("M03", "Search bounds", "Binary search", "Any update works.", "hi = mid", "Shrink it.")));
    private final StudyTasks tasks = new StudyTasks(List.of(
            new StudyTask("list-insert", 1, "List", "Insert a node."),
            new StudyTask("binary-search", 2, "Search", "Implement binary search.")));
    private final List<StudyRecord> logged = new ArrayList<>();
    private final StudyLog log = new StudyLog() {
        @Override
        public int countAttempts(String participantId, String taskId) {
            return (int) logged.stream()
                    .filter(r -> r.participantId().equals(participantId) && r.taskId().equals(taskId))
                    .count();
        }

        @Override
        public void append(StudyRecord record) {
            logged.add(record);
        }
    };
    private final AtomicInteger detectorCalls = new AtomicInteger();
    private final FakeLlmClient llm = new FakeLlmClient();
    private final FeedbackService feedback = new FeedbackService(llm,
            s -> {
                detectorCalls.incrementAndGet();
                return List.of(new Match("M03", "hi = mid", 0.9));
            },
            catalogue, new PromptFactory(), new Anonymiser());
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneOffset.UTC);
    private final StudyService service =
            new StudyService(feedback, tasks, log, new Anonymiser(), clock, "model-x");

    @Test
    void groupAOnTopicOneGetsGenericFeedbackAndNoDetectorCall() {
        StudyResponse response = service.submit("P01", "list-insert", "int x;", "why");

        assertThat(response.feedback()).isEqualTo("FEEDBACK");
        assertThat(response.attempt()).isEqualTo(1);
        assertThat(detectorCalls.get()).isZero();
        assertThat(logged).hasSize(1);
        assertThat(logged.get(0).condition()).isEqualTo(FeedbackCondition.GENERIC);
        assertThat(logged.get(0).matches()).isEmpty();
    }

    @Test
    void groupAOnTopicTwoGetsInformedFeedbackAndMatchesAreLogged() {
        service.submit("P01", "binary-search", "while (lo < hi) { hi = mid; }", "why");

        assertThat(detectorCalls.get()).isEqualTo(1);
        assertThat(logged.get(0).condition()).isEqualTo(FeedbackCondition.INFORMED);
        assertThat(logged.get(0).matches()).extracting(Match::misconceptionId).containsExactly("M03");
        assertThat(logged.get(0).model()).isEqualTo("model-x");
        assertThat(logged.get(0).timestamp()).isEqualTo("2026-11-22T10:00:00Z");
    }

    @Test
    void attemptNumberIncreasesOnResubmission() {
        service.submit("P01", "binary-search", "v1", "why");
        StudyResponse second = service.submit("P01", "binary-search", "v2", "why");

        assertThat(second.attempt()).isEqualTo(2);
        assertThat(logged.get(1).attempt()).isEqualTo(2);
    }

    @Test
    void loggedCodeIsAnonymised() {
        service.submit("P01", "list-insert", "int x; // a.b@example.com", "mail a.b@example.com");

        assertThat(logged.get(0).code()).doesNotContain("a.b@example.com");
        assertThat(logged.get(0).explanation()).doesNotContain("a.b@example.com");
        assertThat(llm.lastUser).doesNotContain("a.b@example.com");
    }

    @Test
    void rejectsBadParticipantIdUnknownTaskAndEmptyCode() {
        assertThatThrownBy(() -> service.submit("Jane Doe", "list-insert", "int x;", "why"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.submit("P01", "nope", "int x;", "why"))
                .isInstanceOf(UnknownTaskException.class);
        assertThatThrownBy(() -> service.submit("P01", "list-insert", " ", "why"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(logged).isEmpty();
    }

    @Test
    void participantIdValidation() {
        assertThat(StudyService.isValidParticipantId("P01")).isTrue();
        assertThat(StudyService.isValidParticipantId("a_b-9")).isTrue();
        assertThat(StudyService.isValidParticipantId("")).isFalse();
        assertThat(StudyService.isValidParticipantId("has space")).isFalse();
        assertThat(StudyService.isValidParticipantId("x".repeat(21))).isFalse();
        assertThat(StudyService.isValidParticipantId(null)).isFalse();
    }
}
