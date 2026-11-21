package dev.rrezart.misconception.feedback;

import dev.rrezart.misconception.model.Submission;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnonymiserTest {

    private final Anonymiser anonymiser = new Anonymiser();

    @Test
    void removesEmailAndAuthorLine() {
        Submission raw = new Submission("P01", "Task",
                "/** @author Jane Doe */\nint x = 1; // mail jane.doe@example.com",
                "Contact me at jane.doe@example.com");

        Submission clean = anonymiser.anonymise(raw);

        assertThat(clean.code()).doesNotContain("Jane Doe").doesNotContain("jane.doe@example.com");
        assertThat(clean.explanation()).doesNotContain("jane.doe@example.com");
        assertThat(clean.id()).isEqualTo("P01");
        assertThat(clean.task()).isEqualTo("Task");
    }

    @Test
    void keepsPlainCodeUnchanged() {
        Submission raw = new Submission("P02", "Task", "int x = 1;", "No personal data.");

        assertThat(anonymiser.anonymise(raw)).isEqualTo(raw);
    }

    @Test
    void handlesNullExplanation() {
        Submission raw = new Submission("P03", "Task", "int x = 1;", null);

        assertThat(anonymiser.anonymise(raw).explanation()).isNull();
    }
}
