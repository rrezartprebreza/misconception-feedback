package dev.rrezart.misconception.collect;

/** The ID is not on the list of IDs the instructor handed out. */
public class UnknownParticipantException extends RuntimeException {

    public UnknownParticipantException(String id) {
        super("Participant id is not on the list: " + id);
    }
}
