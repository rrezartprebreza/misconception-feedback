package dev.rrezart.misconception.study;

import dev.rrezart.misconception.model.FeedbackCondition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConditionAssignerTest {

    @Test
    void groupAGetsGenericThenInformed() {
        assertThat(ConditionAssigner.assign("P01", 1)).isEqualTo(FeedbackCondition.GENERIC);
        assertThat(ConditionAssigner.assign("P01", 2)).isEqualTo(FeedbackCondition.INFORMED);
    }

    @Test
    void groupBGetsInformedThenGeneric() {
        assertThat(ConditionAssigner.assign("P02", 1)).isEqualTo(FeedbackCondition.INFORMED);
        assertThat(ConditionAssigner.assign("P02", 2)).isEqualTo(FeedbackCondition.GENERIC);
    }

    @Test
    void everyParticipantGetsBothConditions() {
        for (int i = 1; i <= 72; i++) {
            String id = String.format("P%02d", i);
            assertThat(ConditionAssigner.assign(id, 1)).isNotEqualTo(ConditionAssigner.assign(id, 2));
        }
    }

    @Test
    void seventyTwoParticipantsSplitEvenly() {
        int genericOnTopicOne = 0;
        for (int i = 1; i <= 72; i++) {
            if (ConditionAssigner.assign(String.format("P%02d", i), 1) == FeedbackCondition.GENERIC) {
                genericOnTopicOne++;
            }
        }
        assertThat(genericOnTopicOne).isEqualTo(36);
    }

    @Test
    void idsWithoutTrailingDigitAreStable() {
        assertThat(ConditionAssigner.assign("alpha", 1)).isEqualTo(ConditionAssigner.assign("alpha", 1));
        assertThat(ConditionAssigner.assign("alpha", 1)).isNotEqualTo(ConditionAssigner.assign("alpha", 2));
    }

    @Test
    void rejectsUnknownTopic() {
        assertThatThrownBy(() -> ConditionAssigner.assign("P01", 3)).isInstanceOf(IllegalArgumentException.class);
    }
}
