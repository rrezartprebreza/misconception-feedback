package dev.rrezart.misconception.llm;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/**
 * Tries the first client; when it fails (for example a rate limit), moves on to the next.
 * A client that failed is skipped for a cooldown period so later requests do not wait for it again.
 * If every client fails, the last error is thrown.
 *
 * <p>Not for use in a controlled study: the model must stay fixed so results are comparable.
 */
public class FallbackLlmClient implements LlmClient {

    private final List<LlmClient> clients;
    private final Duration cooldown;
    private final Clock clock;
    private final Instant[] blockedUntil;

    public FallbackLlmClient(List<LlmClient> clients, Duration cooldown, Clock clock) {
        if (clients.isEmpty()) {
            throw new IllegalArgumentException("At least one client is required");
        }
        this.clients = List.copyOf(clients);
        this.cooldown = cooldown;
        this.clock = clock;
        this.blockedUntil = new Instant[clients.size()];
    }

    @Override
    public String complete(String system, String user) {
        return attempt(c -> c.complete(system, user));
    }

    @Override
    public <T> T completeStructured(String system, String user, Class<T> type) {
        return attempt(c -> c.completeStructured(system, user, type));
    }

    private <R> R attempt(Function<LlmClient, R> call) {
        RuntimeException last = null;
        boolean triedAny = false;
        for (int i = 0; i < clients.size(); i++) {
            if (isBlocked(i)) {
                continue;
            }
            triedAny = true;
            try {
                return call.apply(clients.get(i));
            } catch (RuntimeException e) {
                last = e;
                block(i);
            }
        }
        if (!triedAny) {
            // Everything is cooling down: try them all once more rather than refusing outright.
            for (int i = 0; i < clients.size(); i++) {
                try {
                    R result = call.apply(clients.get(i));
                    unblock(i);
                    return result;
                } catch (RuntimeException e) {
                    last = e;
                    block(i);
                }
            }
        }
        throw last;
    }

    private synchronized boolean isBlocked(int i) {
        return blockedUntil[i] != null && clock.instant().isBefore(blockedUntil[i]);
    }

    private synchronized void block(int i) {
        blockedUntil[i] = clock.instant().plus(cooldown);
    }

    private synchronized void unblock(int i) {
        blockedUntil[i] = null;
    }
}
