package dev.rrezart.misconception.config;

/**
 * The model name written into study logs and pilot result files. {@code STUDY_MODEL_LABEL} wins when set.
 * Otherwise the label is the model of the provider this build uses: the Maven profile decides which
 * provider is on the classpath, so the label never names a model that was not called.
 */
public record ModelLabel(String value) {

    public static ModelLabel resolve(String configured, boolean openAiProvider, String openAiModel, String ollamaModel) {
        if (configured != null && !configured.isBlank() && !"auto".equalsIgnoreCase(configured.trim())) {
            return new ModelLabel(configured.trim());
        }
        String model = openAiProvider ? openAiModel : ollamaModel;
        return new ModelLabel(model == null || model.isBlank() ? "unspecified" : model.trim());
    }

    /** True when the build includes the OpenAI-compatible provider (built with -Popenai). */
    public static boolean openAiOnClasspath() {
        try {
            Class.forName("org.springframework.ai.openai.OpenAiChatModel", false, ModelLabel.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
