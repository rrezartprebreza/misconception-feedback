package dev.rrezart.misconception.collect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParticipantIdsTest {

    @Test
    void emptyOrMissingListIsOpen() {
        assertThat(ParticipantIds.parse("").isOpen()).isTrue();
        assertThat(ParticipantIds.parse(null).isOpen()).isTrue();
        assertThat(ParticipantIds.open().canonical("x9")).contains("x9");
    }

    @Test
    void rangesKeepTheZeroPadding() {
        ParticipantIds ids = ParticipantIds.parse("S01..S12");

        assertThat(ids.size()).isEqualTo(12);
        assertThat(ids.canonical("S01")).contains("S01");
        assertThat(ids.canonical("s12")).contains("S12");
        assertThat(ids.canonical("S13")).isEmpty();
        assertThat(ids.canonical("S1")).isEmpty();
    }

    @Test
    void mixesRangesAndSingleIdsSeparatedByCommasOrSpaces() {
        ParticipantIds ids = ParticipantIds.parse("A1..A3, B-7  teacher");

        assertThat(ids.size()).isEqualTo(5);
        assertThat(ids.canonical(" b-7 ")).contains("B-7");
        assertThat(ids.canonical("Teacher")).contains("teacher");
        assertThat(ids.canonical(null)).isEmpty();
    }

    @Test
    void rangeWithoutRepeatedPrefixWorks() {
        assertThat(ParticipantIds.parse("P001..120").size()).isEqualTo(120);
    }

    @Test
    void badSpecsFailLoudly() {
        assertThatThrownBy(() -> ParticipantIds.parse("S10..S01")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ParticipantIds.parse("S..S5")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ParticipantIds.parse("bad id!")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ParticipantIds.parse("S1..S999999")).isInstanceOf(IllegalArgumentException.class);
    }
}
