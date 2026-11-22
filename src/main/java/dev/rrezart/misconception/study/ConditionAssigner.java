package dev.rrezart.misconception.study;

import dev.rrezart.misconception.model.FeedbackCondition;

/**
 * Counterbalanced crossover assignment. Every participant gets generic feedback on one topic and
 * informed feedback on the other, and half the participants get them in the reverse order.
 *
 * <p>Group A (participant ids ending in an odd digit, such as P01): topic 1 generic, topic 2 informed.
 * Group B (ids ending in an even digit, such as P02): topic 1 informed, topic 2 generic. Ids that do
 * not end in a digit are split by a stable hash. Assigning P01..P72 gives exactly 36 per group.
 */
public final class ConditionAssigner {

    private ConditionAssigner() {
    }

    public static FeedbackCondition assign(String participantId, int topic) {
        if (topic != 1 && topic != 2) {
            throw new IllegalArgumentException("Topic must be 1 or 2, was " + topic);
        }
        boolean groupA = isGroupA(participantId);
        boolean generic = (topic == 1) == groupA;
        return generic ? FeedbackCondition.GENERIC : FeedbackCondition.INFORMED;
    }

    static boolean isGroupA(String participantId) {
        char last = participantId.charAt(participantId.length() - 1);
        if (Character.isDigit(last)) {
            return (last - '0') % 2 == 1;
        }
        return Math.floorMod(participantId.hashCode(), 2) == 1;
    }
}
