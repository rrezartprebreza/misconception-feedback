package dev.rrezart.misconception.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.model.Match;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PilotEvaluatorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final MisconceptionCatalogue catalogue = MisconceptionCatalogue.fromClasspath(mapper, "/catalogue.json");

    private List<GoldItem> syntheticSet() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/pilot/synthetic-gold.json")) {
            return mapper.readValue(in, new TypeReference<List<GoldItem>>() {
            });
        }
    }

    @Test
    void syntheticSetOnlyUsesCatalogueIds() throws IOException {
        for (GoldItem item : syntheticSet()) {
            for (String label : item.labels()) {
                assertThat(catalogue.find(label)).as(item.id() + " label " + label).isPresent();
            }
        }
    }

    @Test
    void aPerfectDetectorScoresOne() throws IOException {
        List<GoldItem> items = syntheticSet();

        PilotEvaluator.Report report = PilotEvaluator.evaluate(items, submission -> items.stream()
                .filter(i -> i.id().equals(submission.id()))
                .findFirst().orElseThrow()
                .labels().stream()
                .map(id -> new Match(id, "evidence", 1.0))
                .toList(), catalogue);

        assertThat(report.items()).isEqualTo(items.size());
        assertThat(report.micro().f1()).isCloseTo(1.0, within(1e-9));
        assertThat(report.kappaById().values()).allSatisfy(k -> assertThat(k).isCloseTo(1.0, within(1e-9)));
    }

    @Test
    void aDetectorThatFindsNothingHasZeroRecall() throws IOException {
        PilotEvaluator.Report report = PilotEvaluator.evaluate(syntheticSet(), s -> List.of(), catalogue);

        assertThat(report.micro().recall()).isZero();
        assertThat(PilotEvaluator.format(report)).contains("Items: 12");
    }
}
