package dev.rrezart.misconception.eval;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class AgreementMetricsTest {

    @Test
    void microScoresCountHitsMissesAndExtras() {
        List<Set<String>> predicted = List.of(Set.of("M01"), Set.of("M02", "M03"));
        List<Set<String>> gold = List.of(Set.of("M01"), Set.of("M02"));

        AgreementMetrics.Scores scores = AgreementMetrics.microScores(predicted, gold);

        // tp = 2, fp = 1, fn = 0
        assertThat(scores.precision()).isCloseTo(2.0 / 3.0, within(1e-9));
        assertThat(scores.recall()).isCloseTo(1.0, within(1e-9));
        assertThat(scores.f1()).isCloseTo(0.8, within(1e-9));
    }

    @Test
    void microScoresOfNothingAreZero() {
        AgreementMetrics.Scores scores = AgreementMetrics.microScores(List.of(Set.of()), List.of(Set.of()));

        assertThat(scores.precision()).isZero();
        assertThat(scores.recall()).isZero();
        assertThat(scores.f1()).isZero();
    }

    @Test
    void microScoresRejectMismatchedSizes() {
        assertThatThrownBy(() -> AgreementMetrics.microScores(List.of(Set.of()), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void kappaIsOneForPerfectAgreement() {
        boolean[] ratings = {true, false, true, false};

        assertThat(AgreementMetrics.cohensKappa(ratings, ratings)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void kappaMatchesHandCalculation() {
        // observed 3/4, expected 0.5 * 0.25 + 0.5 * 0.75 = 0.5, kappa = (0.75 - 0.5) / 0.5 = 0.5
        boolean[] a = {true, true, false, false};
        boolean[] b = {true, false, false, false};

        assertThat(AgreementMetrics.cohensKappa(a, b)).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void kappaRejectsEmptyOrUnequalInput() {
        assertThatThrownBy(() -> AgreementMetrics.cohensKappa(new boolean[0], new boolean[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AgreementMetrics.cohensKappa(new boolean[1], new boolean[2]))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
