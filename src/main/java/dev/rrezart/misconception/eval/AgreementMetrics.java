package dev.rrezart.misconception.eval;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Metrics for RQ1: how closely detector labels match instructor labels.
 */
public final class AgreementMetrics {

    private AgreementMetrics() {
    }

    public record Scores(double precision, double recall, double f1) {
    }

    /**
     * Micro-averaged precision, recall and F1 over all submissions. Each list entry holds the set of
     * misconception ids for one submission.
     */
    public static Scores microScores(List<Set<String>> predicted, List<Set<String>> gold) {
        if (predicted.size() != gold.size()) {
            throw new IllegalArgumentException("Predicted and gold lists must have the same size");
        }
        int tp = 0;
        int fp = 0;
        int fn = 0;
        for (int i = 0; i < predicted.size(); i++) {
            Set<String> p = predicted.get(i);
            Set<String> g = gold.get(i);
            Set<String> hit = new HashSet<>(p);
            hit.retainAll(g);
            tp += hit.size();
            fp += p.size() - hit.size();
            fn += g.size() - hit.size();
        }
        double precision = tp + fp == 0 ? 0.0 : (double) tp / (tp + fp);
        double recall = tp + fn == 0 ? 0.0 : (double) tp / (tp + fn);
        double f1 = precision + recall == 0 ? 0.0 : 2 * precision * recall / (precision + recall);
        return new Scores(precision, recall, f1);
    }

    /**
     * Cohen's kappa for two binary ratings of the same items, for example "misconception M03 present"
     * according to the detector and according to an instructor.
     */
    public static double cohensKappa(boolean[] a, boolean[] b) {
        if (a.length != b.length || a.length == 0) {
            throw new IllegalArgumentException("Ratings must be non-empty and the same length");
        }
        int n = a.length;
        int agree = 0;
        int aTrue = 0;
        int bTrue = 0;
        for (int i = 0; i < n; i++) {
            if (a[i] == b[i]) {
                agree++;
            }
            if (a[i]) {
                aTrue++;
            }
            if (b[i]) {
                bTrue++;
            }
        }
        double observed = (double) agree / n;
        double pa = (double) aTrue / n;
        double pb = (double) bTrue / n;
        double expected = pa * pb + (1 - pa) * (1 - pb);
        if (expected == 1.0) {
            return observed == 1.0 ? 1.0 : 0.0;
        }
        return (observed - expected) / (1 - expected);
    }
}
