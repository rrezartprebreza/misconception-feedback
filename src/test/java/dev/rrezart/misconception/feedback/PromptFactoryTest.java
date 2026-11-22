package dev.rrezart.misconception.feedback;

import dev.rrezart.misconception.catalogue.Misconception;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.model.FeedbackCondition;
import dev.rrezart.misconception.model.Match;
import dev.rrezart.misconception.model.ProgrammingLanguage;
import dev.rrezart.misconception.model.Submission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptFactoryTest {

    private final PromptFactory prompts = new PromptFactory();
    private final MisconceptionCatalogue catalogue = new MisconceptionCatalogue(List.of(
            new Misconception("M03", "Search bounds", "Binary search",
                    "Any update of lo and hi works.", "hi = mid", "Make each iteration shrink the interval.")));
    private final Submission submission = new Submission("P01", "Implement binary search.",
            "while (lo < hi) { hi = mid; }", "It halves the range.");

    @Test
    void genericPromptHasTaskCodeAndExplanationButNoMisconception() {
        String text = prompts.feedbackUser(submission, FeedbackCondition.GENERIC,
                List.of(new Match("M03", "hi = mid", 0.9)), catalogue);

        assertThat(text).contains("Implement binary search.", "while (lo < hi)", "It halves the range.");
        assertThat(text).doesNotContain("Search bounds").doesNotContain("shrink the interval");
    }

    @Test
    void informedPromptAddsBeliefAndCorrection() {
        String text = prompts.feedbackUser(submission, FeedbackCondition.INFORMED,
                List.of(new Match("M03", "hi = mid", 0.9)), catalogue);

        assertThat(text).contains("Search bounds");
        assertThat(text).contains("any update of lo and hi works.");
        assertThat(text).contains("Make each iteration shrink the interval.");
    }

    @Test
    void informedWithNoMatchesEqualsGeneric() {
        String generic = prompts.feedbackUser(submission, FeedbackCondition.GENERIC, List.of(), catalogue);
        String informed = prompts.feedbackUser(submission, FeedbackCondition.INFORMED, List.of(), catalogue);

        assertThat(informed).isEqualTo(generic);
    }

    @Test
    void missingExplanationIsMarkedNone() {
        Submission noExplanation = new Submission("P02", "Task", "int x;", null);

        assertThat(prompts.detectionUser(noExplanation)).contains("(none)");
    }

    @Test
    void detectionPromptListsCatalogueIds() {
        assertThat(prompts.detectionSystem(catalogue)).contains("M03").contains("Binary search");
    }
    @Test
    void promptsNameTheConfiguredLanguageAndUseItsFence() {
        PromptFactory python = new PromptFactory(ProgrammingLanguage.PYTHON);

        assertThat(python.detectionSystem(catalogue)).contains("Python");
        assertThat(python.feedbackSystem()).contains("Python");
        assertThat(python.detectionUser(submission)).contains("```python");
    }

    @Test
    void csharpUsesItsDisplayNameAndFence() {
        PromptFactory csharp = new PromptFactory(ProgrammingLanguage.CSHARP);

        assertThat(csharp.detectionSystem(catalogue)).contains("C#");
        assertThat(csharp.detectionUser(submission)).contains("```csharp");
    }

    @Test
    void defaultConstructorKeepsJava() {
        assertThat(prompts.detectionUser(submission)).contains("```java");
    }
}
