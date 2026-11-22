package dev.rrezart.misconception.llm;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FallbackLlmClientTest {

    static class Stub implements LlmClient {
        final String name;
        boolean failing;
        int calls;

        Stub(String name, boolean failing) {
            this.name = name;
            this.failing = failing;
        }

        @Override
        public String complete(String system, String user) {
            calls++;
            if (failing) {
                throw new IllegalStateException(name + " rate limited");
            }
            return name;
        }

        @Override
        public <T> T completeStructured(String system, String user, Class<T> type) {
            calls++;
            if (failing) {
                throw new IllegalStateException(name + " rate limited");
            }
            return type.cast(name);
        }
    }

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();

    @Test
    void usesTheFirstClientWhenItWorks() {
        Stub a = new Stub("a", false);
        Stub b = new Stub("b", false);
        FallbackLlmClient client = new FallbackLlmClient(List.of(a, b), Duration.ofSeconds(60), clock);

        assertThat(client.complete("s", "u")).isEqualTo("a");
        assertThat(b.calls).isZero();
    }

    @Test
    void movesToTheNextClientWhenTheFirstFails() {
        Stub a = new Stub("a", true);
        Stub b = new Stub("b", false);
        FallbackLlmClient client = new FallbackLlmClient(List.of(a, b), Duration.ofSeconds(60), clock);

        assertThat(client.complete("s", "u")).isEqualTo("b");
        assertThat(client.completeStructured("s", "u", String.class)).isEqualTo("b");
    }

    @Test
    void skipsAFailedClientDuringCooldownThenTriesItAgain() {
        Stub a = new Stub("a", true);
        Stub b = new Stub("b", false);
        FallbackLlmClient client = new FallbackLlmClient(List.of(a, b), Duration.ofSeconds(60), clock);

        client.complete("s", "u");
        client.complete("s", "u");
        assertThat(a.calls).isEqualTo(1);

        a.failing = false;
        clock.now = clock.now.plusSeconds(61);
        assertThat(client.complete("s", "u")).isEqualTo("a");
    }

    @Test
    void throwsTheLastErrorWhenEveryClientFails() {
        FallbackLlmClient client = new FallbackLlmClient(
                List.of(new Stub("a", true), new Stub("b", true)), Duration.ofSeconds(60), clock);

        assertThatThrownBy(() -> client.complete("s", "u"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("b rate limited");
    }

    @Test
    void stillTriesEveryClientWhenAllAreCoolingDown() {
        Stub a = new Stub("a", true);
        Stub b = new Stub("b", true);
        FallbackLlmClient client = new FallbackLlmClient(List.of(a, b), Duration.ofSeconds(60), clock);
        assertThatThrownBy(() -> client.complete("s", "u")).isInstanceOf(IllegalStateException.class);

        b.failing = false;

        assertThat(client.complete("s", "u")).isEqualTo("b");
    }

    @Test
    void needsAtLeastOneClient() {
        assertThatThrownBy(() -> new FallbackLlmClient(List.of(), Duration.ofSeconds(1), clock))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
