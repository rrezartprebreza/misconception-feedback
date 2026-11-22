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

    /** What the detector said about one item next to the instructor's labels, for error analysis. */
    public record ItemResult(String id, List<String> gold, List<String> predicted, List<Match> matches) {
    }

    /** Counts and scores for one misconception, treating each answer as one yes/no decision. */
    public record PerMisconception(int truePositives, int falsePositives, int falseNegatives,
                                   double precision, double recall, double f1) {
    }

    /** The summary plus everything behind it. */
    public record Detailed(Report report, List<ItemResult> items, Map<String, PerMisconception> perMisconception) {
    }

    public static Report evaluate(List<GoldItem> items,
                                  Function<Submission, List<Match>> detector,
                                  MisconceptionCatalogue catalogue) {
        return evaluateDetailed(items, detector, catalogue).report();
    }

    /** Same as {@link #evaluate} but also keeps each item's prediction. The detector is called once per item. */
    public static Detailed evaluateDetailed(List<GoldItem> items,
                                            Function<Submission, List<Match>> detector,
                                            MisconceptionCatalogue catalogue) {
        List<Set<String>> predicted = new ArrayList<>();
        List<Set<String>> gold = new ArrayList<>();
        List<ItemResult> results = new ArrayList<>();
        for (GoldItem item : items) {
            List<Match> matches = detector.apply(item.toSubmission());
            Set<String> ids = new HashSet<>();
            for (Match match : matches) {
                ids.add(match.misconceptionId());
            }
            predicted.add(ids);
            gold.add(new HashSet<>(item.labels()));
            results.add(new ItemResult(item.id(), sorted(item.labels()), sorted(ids), matches));
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
        Report report = new Report(items.size(), AgreementMetrics.microScores(predicted, gold), kappaById);

        Map<String, PerMisconception> perId = new LinkedHashMap<>();
        for (Misconception m : catalogue.all()) {
            int tp = 0;
            int fp = 0;
            int fn = 0;
            for (int i = 0; i < items.size(); i++) {
                boolean said = predicted.get(i).contains(m.id());
                boolean truth = gold.get(i).contains(m.id());
                if (said && truth) {
                    tp++;
                } else if (said) {
                    fp++;
                } else if (truth) {
                    fn++;
                }
            }
            if (tp + fp + fn > 0) {
                double precision = tp + fp == 0 ? 0 : (double) tp / (tp + fp);
                double recall = tp + fn == 0 ? 0 : (double) tp / (tp + fn);
                double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
                perId.put(m.id(), new PerMisconception(tp, fp, fn, precision, recall, f1));
            }
        }
        return new Detailed(report, results, perId);
    }

    private static List<String> sorted(java.util.Collection<String> ids) {
        return ids.stream().sorted().toList();
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

    /** The summary followed by a per-misconception table and a list of the items where detector and instructor differ. */
    public static String formatDetailed(Detailed detailed) {
        StringBuilder sb = new StringBuilder(format(detailed.report()));
        sb.append("Per misconception (tp / fp / fn, precision, recall, F1):\n");
        detailed.perMisconception().forEach((id, c) -> sb.append(String.format("  %s  %d / %d / %d   %.2f  %.2f  %.2f%n",
                id, c.truePositives(), c.falsePositives(), c.falseNegatives(), c.precision(), c.recall(), c.f1())));
        sb.append("Items where the detector and the instructor differ:\n");
        int differing = 0;
        for (ItemResult r : detailed.items()) {
            if (!r.gold().equals(r.predicted())) {
                differing++;
                sb.append(String.format("  %s  instructor %s, detector %s%n", r.id(), r.gold(), r.predicted()));
            }
        }
        if (differing == 0) {
            sb.append("  none\n");
        }
        return sb.toString();
    }
}
