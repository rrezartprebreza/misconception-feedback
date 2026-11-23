package dev.rrezart.misconception.collect;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The IDs students may use. Empty means open: any valid ID is accepted. Giving the list keeps
 * names, initials and student numbers out of the data, because only IDs you handed out get in.
 *
 * <p>The list is comma or space separated. {@code S01..S60} stands for S01, S02, ... S60 (the zero padding of
 * the first number is kept). Matching ignores case and returns the ID as written in the list.
 */
public final class ParticipantIds {

    private static final int MAX_RANGE = 5_000;

    private final Map<String, String> byLowerCase;

    private ParticipantIds(Map<String, String> byLowerCase) {
        this.byLowerCase = byLowerCase;
    }

    public static ParticipantIds open() {
        return new ParticipantIds(Map.of());
    }

    public static ParticipantIds parse(String spec) {
        Map<String, String> ids = new LinkedHashMap<>();
        if (spec != null) {
            for (String token : spec.trim().split("[,\\s]+")) {
                if (token.isBlank()) {
                    continue;
                }
                int dots = token.indexOf("..");
                if (dots < 0) {
                    add(ids, token);
                } else {
                    addRange(ids, token, dots);
                }
            }
        }
        return new ParticipantIds(ids);
    }

    private static void addRange(Map<String, String> ids, String token, int dots) {
        String from = token.substring(0, dots);
        String to = token.substring(dots + 2);
        int digitsStart = from.length();
        while (digitsStart > 0 && Character.isDigit(from.charAt(digitsStart - 1))) {
            digitsStart--;
        }
        String prefix = from.substring(0, digitsStart);
        String startDigits = from.substring(digitsStart);
        String endDigits = to.startsWith(prefix) ? to.substring(prefix.length()) : to;
        if (startDigits.isEmpty() || endDigits.isEmpty() || !endDigits.chars().allMatch(Character::isDigit)
                || startDigits.length() > 9 || endDigits.length() > 9) {
            throw new IllegalArgumentException("Cannot read the ID range '" + token + "'. Use a form like S01..S60");
        }
        int start = Integer.parseInt(startDigits);
        int end = Integer.parseInt(endDigits);
        if (end < start || end - start >= MAX_RANGE) {
            throw new IllegalArgumentException("Cannot read the ID range '" + token + "'. Use a form like S01..S60");
        }
        for (int n = start; n <= end; n++) {
            add(ids, prefix + String.format("%0" + startDigits.length() + "d", n));
        }
    }

    private static void add(Map<String, String> ids, String id) {
        if (!dev.rrezart.misconception.study.StudyService.isValidParticipantId(id)) {
            throw new IllegalArgumentException("'" + id + "' is not a valid participant id (1 to 20 letters, digits, - or _)");
        }
        ids.putIfAbsent(id.toLowerCase(Locale.ROOT), id);
    }

    public boolean isOpen() {
        return byLowerCase.isEmpty();
    }

    public int size() {
        return byLowerCase.size();
    }

    /** The ID as listed, or empty when it is not allowed. In an open list any ID is returned unchanged. */
    public Optional<String> canonical(String id) {
        if (id == null) {
            return Optional.empty();
        }
        if (isOpen()) {
            return Optional.of(id);
        }
        return Optional.ofNullable(byLowerCase.get(id.trim().toLowerCase(Locale.ROOT)));
    }
}
