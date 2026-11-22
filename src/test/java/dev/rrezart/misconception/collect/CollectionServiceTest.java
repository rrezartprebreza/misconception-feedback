package dev.rrezart.misconception.collect;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.eval.GoldItem;
import dev.rrezart.misconception.eval.PilotEvaluator;
import dev.rrezart.misconception.feedback.Anonymiser;
import dev.rrezart.misconception.model.ProgrammingLanguage;
import dev.rrezart.misconception.study.StudyTasks;
import dev.rrezart.misconception.study.UnknownTaskException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class CollectionServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final MisconceptionCatalogue catalogue = MisconceptionCatalogue.fromClasspath(mapper, "/catalogue-java.json");
    private final StudyTasks tasks = StudyTasks.fromClasspath(mapper, "/study/tasks-java.json");

    @TempDir
    Path dir;

    private AnswerStore store;
    private CollectionService service;

    @BeforeEach
    void setUp() {
        store = new AnswerStore(dir.resolve("answers.jsonl"), dir.resolve("labels.jsonl"), mapper);
        service = new CollectionService(store, tasks, catalogue, new Anonymiser(), ProgrammingLanguage.JAVA, clock);
    }

    @Test
    void storesAnonymisedAnswersWithSequentialIds() {
        CollectedAnswer a = service.submit("P01", "binary-search", "// @author Ana ana@x.com\nint x;", "mail ana@x.com");

        assertThat(a.id()).isEqualTo("A0001");
        assertThat(a.language()).isEqualTo("java");
        assertThat(a.code()).doesNotContain("ana@x.com").doesNotContain("Ana");
        assertThat(a.explanation()).doesNotContain("@x.com");
        assertThat(service.submit("P02", "binary-search", "code", null).id()).isEqualTo("A0002");
    }

    @Test
    void rejectsBadInput() {
        assertThatThrownBy(() -> service.submit("bad id!", "binary-search", "c", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.submit("P1", "nope", "c", null)).isInstanceOf(UnknownTaskException.class);
        assertThatThrownBy(() -> service.submit("P1", "binary-search", " ", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.submit("P1", "binary-search", "x".repeat(20_001), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void latestLabelWinsAndNoneIsDifferentFromUnlabelled() {
        service.submit("P01", "binary-search", "a", null);
        service.submit("P02", "binary-search", "b", null);
        service.submit("P03", "binary-search", "c", null);
        service.label("ana", "A0001", List.of("M03"));
        service.label("ana", "A0001", List.of("M03", "M01", "M01"));
        service.label("ana", "A0002", List.of());

        List<GoldItem> gold = service.exportGold("ana");

        assertThat(gold).hasSize(2);
        assertThat(Set.copyOf(gold.get(0).labels())).containsExactlyInAnyOrder("M03", "M01");
        assertThat(gold.get(1).labels()).isEmpty();
        assertThat(store.labelsOf("ana", "A0003")).isEmpty();
    }

    @Test
    void rejectsUnknownIdsAnswersAndLabellerNames() {
        service.submit("P01", "binary-search", "a", null);

        assertThatThrownBy(() -> service.label("ana", "A0001", List.of("M99"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.label("ana", "A9999", List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.label("bad name!", "A0001", List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void agreementUsesOnlyAnswersBothLabelled() {
        service.submit("P01", "binary-search", "a", null);
        service.submit("P02", "binary-search", "b", null);
        service.submit("P03", "binary-search", "c", null);
        service.label("ana", "A0001", List.of("M03"));
        service.label("ana", "A0002", List.of());
        service.label("ana", "A0003", List.of("M01"));
        service.label("bo", "A0001", List.of("M03"));
        service.label("bo", "A0002", List.of());

        PilotEvaluator.Report report = service.agreement("ana", "bo");

        assertThat(report.items()).isEqualTo(2);
        assertThat(report.micro().f1()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void dataSurvivesARestart() {
        service.submit("P01", "binary-search", "a", null);
        service.label("ana", "A0001", List.of("M03"));

        AnswerStore reopened = new AnswerStore(dir.resolve("answers.jsonl"), dir.resolve("labels.jsonl"), mapper);
        CollectionService again = new CollectionService(reopened, tasks, catalogue, new Anonymiser(), ProgrammingLanguage.JAVA, clock);

        assertThat(reopened.all()).hasSize(1);
        assertThat(reopened.labelsOf("ana", "A0001")).contains(List.of("M03"));
        assertThat(again.submit("P02", "binary-search", "b", null).id()).isEqualTo("A0002");
    }

    @Test
    void exportOnlyIncludesAnswersInTheServicesLanguage() {
        CollectionService python = new CollectionService(store, StudyTasks.fromClasspath(mapper, "/study/tasks-python.json"),
                MisconceptionCatalogue.fromClasspath(mapper, "/catalogue-python.json"), new Anonymiser(),
                ProgrammingLanguage.PYTHON, clock);
        service.submit("P01", "binary-search", "java code", null);
        python.submit("P02", "binary-search", "python code", null);
        service.label("ana", "A0001", List.of("M03"));
        service.label("ana", "A0002", List.of("M03"));

        assertThat(service.exportGold("ana")).extracting(GoldItem::code).containsExactly("java code");
        assertThat(python.exportGold("ana")).extracting(GoldItem::code).containsExactly("python code");
    }
}
