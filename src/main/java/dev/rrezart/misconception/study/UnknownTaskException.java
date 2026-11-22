package dev.rrezart.misconception.study;

public class UnknownTaskException extends RuntimeException {

    public UnknownTaskException(String taskId) {
        super("Unknown task: " + taskId);
    }
}
