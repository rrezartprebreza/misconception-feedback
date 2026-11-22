package dev.rrezart.misconception.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.model.Match;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultsWriterTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void savesScoresAndEveryPredictionWithoutTheStudentCode(@TempDir Path dir) throws IOException {
        MisconceptionCatalogue catalogue = MisconceptionCatalogue.fromClasspath(mapper, "/catalogue-java.json");
        List<GoldItem> items = List.of(new GoldItem("A0001", "task", "SECRET STUDENT CODE", "", List.of("M03")));
        PilotEvaluator.Detailed detailed = PilotEvaluator.evaluateDetailed(items,
                s -> List.of(new Match("M03", "lo = mid", 0.9)), catalogue);

        Path json = ResultsWriter.write(mapper, dir.resolve("results"), "java", "openai/gpt-oss-20b",
                Instant.parse("2026-11-23T09:30:15Z"), detailed);

        assertThat(json.getFileName().toString()).isEqualTo("java-openai_gpt-oss-20b-20261123T093015Z.json");
        JsonNode root = mapper.readTree(json.toFile());
        assertThat(root.get("model").asText()).isEqualTo("openai/gpt-oss-20b");
        assertThat(root.get("micro").get("f1").asDouble()).isEqualTo(1.0);
        assertThat(root.get("results").get(0).get("predicted").get(0).asText()).isEqualTo("M03");
        assertThat(root.get("results").get(0).get("matches").get(0).get("evidence").asText()).isEqualTo("lo = mid");
        assertThat(Files.readString(json)).doesNotContain("SECRET STUDENT CODE");
        assertThat(Files.readString(Path.of(json.toString().replace(".json", ".txt")))).contains("Items: 1");
    }

    @Test
    void modelLabelsBecomeSafeFileNames() {
        assertThat(ResultsWriter.safe("qwen2.5-coder:7b")).isEqualTo("qwen2.5-coder_7b");
        assertThat(ResultsWriter.safe("  ")).isEqualTo("unspecified");
        assertThat(ResultsWriter.safe(null)).isEqualTo("unspecified");
        assertThat(ResultsWriter.safe("x".repeat(100))).hasSize(60);
    }
}
