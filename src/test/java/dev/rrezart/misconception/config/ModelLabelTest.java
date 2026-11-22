package dev.rrezart.misconception.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelLabelTest {

    @Test
    void anExplicitLabelWins() {
        assertThat(ModelLabel.resolve("my-label", true, "openai/gpt-oss-20b", "qwen").value()).isEqualTo("my-label");
    }

    @Test
    void autoUsesTheModelOfTheProviderInTheBuild() {
        assertThat(ModelLabel.resolve("auto", true, "openai/gpt-oss-20b", "qwen2.5-coder:7b").value()).isEqualTo("openai/gpt-oss-20b");
        assertThat(ModelLabel.resolve("auto", false, "openai/gpt-oss-20b", "qwen2.5-coder:7b").value()).isEqualTo("qwen2.5-coder:7b");
        assertThat(ModelLabel.resolve("", true, " llama-3.3-70b-versatile ", "x").value()).isEqualTo("llama-3.3-70b-versatile");
    }

    @Test
    void aMissingModelIsReportedAsUnspecified() {
        assertThat(ModelLabel.resolve(null, true, "", "x").value()).isEqualTo("unspecified");
    }
}
