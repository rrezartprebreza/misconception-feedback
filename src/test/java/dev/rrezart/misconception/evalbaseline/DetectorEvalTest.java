package dev.rrezart.misconception.evalbaseline;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.detect.LlmDetector;
import dev.rrezart.misconception.eval.GoldItem;
import dev.rrezart.misconception.feedback.PromptFactory;
import dev.rrezart.misconception.llm.SpringAiLlmClient;
import dev.rrezart.misconception.model.Match;
import dev.rrezart.misconception.model.ProgrammingLanguage;
import io.github.rrezartprebreza.evalbaseline.junit.EvalBaseline;
import io.github.rrezartprebreza.evalbaseline.junit.EvalTest;
import io.github.rrezartprebreza.evalbaseline.junit.EvalTrial;
import io.github.rrezartprebreza.evalbaseline.springai.EvalMode;
import io.github.rrezartprebreza.evalbaseline.springai.RecordingChatModel;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Regression test for the misconception detector, using spring-ai-eval-baseline.
 *
 * <p>Each run checks one answer from the synthetic Java set ({@code pilot/synthetic-gold-java.json},
 * written by the authors; no student data). A run passes when the detector returns exactly the
 * labelled misconceptions. The pass rate is compared with {@code detector-baseline.tsv}, and the
 * test fails only on a statistically significant drop, so a prompt or catalogue change that makes
 * detection worse is caught in CI.
 *
 * <p>CI replays recorded answers, so it needs no network or API key. To record (or re-record after
 * changing the prompt or catalogue), with a local Ollama or with Groq:
 *
 * <pre>
 * EVAL_BASE_URL=http://localhost:11434 EVAL_MODEL=qwen2.5-coder:7b \
 *   mvn test -Dtest=DetectorEvalTest -Deval.mode=record -Deval.update-baseline=true
 *
 * EVAL_BASE_URL=https://api.groq.com/openai EVAL_API_KEY=... EVAL_MODEL=openai/gpt-oss-120b \
 *   mvn test -Dtest=DetectorEvalTest -Deval.mode=record -Deval.update-baseline=true
 * </pre>
 *
 * Then commit {@code src/test/resources/eval/}. Until a recording exists the test is skipped.
 * Latency is not checked because replayed answers take no time.
 */
@EnabledIf("recordingsAvailable")
@EvalBaseline(value = DetectorEvalTest.BASELINE, maxLatencyFactor = 0)
class DetectorEvalTest {

    static final String BASELINE = "src/test/resources/eval/detector-baseline.tsv";
    static final Path CASSETTES = Path.of("src/test/resources/eval/detector-cassettes");
    static final double THRESHOLD = 0.6;

    static final List<GoldItem> GOLD = readGold(ProgrammingLanguage.JAVA.goldResource());
    static final List<GoldItem> WITH_MISCONCEPTION = GOLD.stream().filter(g -> !g.labels().isEmpty()).toList();
    static final List<GoldItem> CORRECT = GOLD.stream().filter(g -> g.labels().isEmpty()).toList();

    private static LlmDetector detector;

    /** One run per synthetic answer that shows a misconception (9 answers). */
    @EvalTest(runs = 9)
    void findsTheLabelledMisconceptions(EvalTrial trial) {
        check(WITH_MISCONCEPTION.get(trial.runIndex()), trial);
    }

    /** One run per correct synthetic answer (3 answers): no false alarm allowed. */
    @EvalTest(runs = 3)
    void staysSilentOnCorrectAnswers(EvalTrial trial) {
        check(CORRECT.get(trial.runIndex()), trial);
    }

    private static void check(GoldItem item, EvalTrial trial) {
        Set<String> expected = new TreeSet<>(item.labels());
        Set<String> found = detector().detect(item.toSubmission()).stream()
                .map(Match::misconceptionId)
                .collect(Collectors.toCollection(TreeSet::new));
        trial.check(found.equals(expected), item.id() + ": expected " + expected + " but found " + found);
    }

    private static synchronized LlmDetector detector() {
        if (detector == null) {
            EvalMode mode = EvalMode.fromEnvironment(EvalMode.REPLAY);
            ChatModel real = mode == EvalMode.REPLAY ? null : OpenAiCompatibleChatModel.fromEnvironment();
            ChatModel model = new RecordingChatModel(real, CASSETTES, mode);
            ObjectMapper mapper = new ObjectMapper();
            ProgrammingLanguage java = ProgrammingLanguage.JAVA;
            detector = new LlmDetector(
                    new SpringAiLlmClient(ChatClient.builder(model)),
                    MisconceptionCatalogue.fromClasspath(mapper, java.catalogueResource()),
                    new PromptFactory(java),
                    THRESHOLD);
        }
        return detector;
    }

    /** Skipped in replay mode until answers have been recorded once. */
    static boolean recordingsAvailable() {
        if (EvalMode.fromEnvironment(EvalMode.REPLAY) != EvalMode.REPLAY) {
            return true;
        }
        if (!Files.isDirectory(CASSETTES)) {
            return false;
        }
        try (Stream<Path> files = Files.list(CASSETTES)) {
            return files.anyMatch(p -> p.toString().endsWith(".json"));
        } catch (IOException e) {
            return false;
        }
    }

    private static List<GoldItem> readGold(String resource) {
        try (InputStream in = DetectorEvalTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + resource);
            }
            return new ObjectMapper().readValue(in, new TypeReference<List<GoldItem>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
