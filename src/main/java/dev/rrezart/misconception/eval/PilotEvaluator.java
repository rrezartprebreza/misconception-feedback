package dev.rrezart.misconception.eval;

import dev.rrezart.misconception.catalogue.Misconception;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.model.Match;
import dev.rrezart.misconception.model.Submission;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Runs a detector over a gold-labelled set and summarises agreement (RQ1). Pure logic: the detector
 * is passed in as a function, so it can be tested without a model.
 */
public final class PilotEvaluator {

    private PilotEvaluator() {
    }

    public record Report(int items, AgreementMetrics.Scores micro, Map<String, Double> kappaById) {
    }

    public static Report evaluate(List<GoldItem> items,
                                  Function<Submission, List<Match>> detector,
                                  MisconceptionCatalogue catalogue) {
        List<Set<String>> predicted = new ArrayList<>();
        List<Set<String>> gold = new ArrayList<>();
        for (GoldItem item : items) {
            Set<String> ids = new HashSet<>();
            for (Match match : detector.apply(item.toSubmission())) {
                ids.add(match.misconceptionId());
            }
            predicted.add(ids);
            gold.add(new HashSet<>(item.labels()));
        }

        Map<String, Double> kappaById = new LinkedHashMap<>();
        for (Misconception m : catalogue.all()) {
            boolean[] byDetector = new boolean[items.size()];
            boolean[] byInstructor = new boolean[items.size()];
            boolean seen = false;
            for (int i = 0; i < items.size(); i++) {
                byDetector[i] = predicted.get(i).contains(m.id());
                byInstructor[i] = gold.get(i).contains(m.id());
                seen |= byDetector[i] || byInstructor[i];
            }
            if (seen) {
                kappaById.put(m.id(), AgreementMetrics.cohensKappa(byDetector, byInstructor));
            }
        }
        return new Report(items.size(), AgreementMetrics.microScores(predicted, gold), kappaById);
    }

    public static String format(Report report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Items: ").append(report.items()).append('\n');
        sb.append(String.format("Micro precision %.2f, recall %.2f, F1 %.2f%n",
                report.micro().precision(), report.micro().recall(), report.micro().f1()));
        sb.append("Cohen's kappa per misconception (only ids that appear in gold or predicted labels):\n");
        report.kappaById().forEach((id, kappa) -> sb.append(String.format("  %s  %.2f%n", id, kappa)));
        return sb.toString();
    }
}
