package dev.rrezart.misconception.study;

import dev.rrezart.misconception.feedback.Anonymiser;
import dev.rrezart.misconception.feedback.FeedbackService;
import dev.rrezart.misconception.model.FeedbackCondition;
import dev.rrezart.misconception.model.FeedbackResult;
import dev.rrezart.misconception.model.Submission;

import java.time.Clock;
import java.util.regex.Pattern;

/**
 * Runs one study submission: assigns the condition from the participant id and task topic,
 * produces feedback, logs everything and returns only the feedback text and attempt number.
 */
public class StudyService {

    private static final Pattern PARTICIPANT_ID = Pattern.compile("[A-Za-z0-9_-]{1,20}");

    private final FeedbackService feedback;
    private final StudyTasks tasks;
    private final StudyLog log;
    private final Anonymiser anonymiser;
    private final Clock clock;
    private final String modelLabel;

    public StudyService(FeedbackService feedback,
                        StudyTasks tasks,
                        StudyLog log,
                        Anonymiser anonymiser,
                        Clock clock,
                        String modelLabel) {
        this.feedback = feedback;
        this.tasks = tasks;
        this.log = log;
        this.anonymiser = anonymiser;
        this.clock = clock;
        this.modelLabel = modelLabel;
    }

    public static boolean isValidParticipantId(String id) {
        return id != null && PARTICIPANT_ID.matcher(id).matches();
    }

    public StudyResponse submit(String participantId, String taskId, String code, String explanation) {
        if (!isValidParticipantId(participantId)) {
            throw new IllegalArgumentException("Participant id must be 1 to 20 letters, digits, - or _");
        }
        StudyTask task = tasks.find(taskId).orElseThrow(() -> new UnknownTaskException(taskId));
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }

        FeedbackCondition condition = ConditionAssigner.assign(participantId, task.topic());
        Submission clean = anonymiser.anonymise(
                new Submission(participantId + "/" + task.id(), task.text(), code, explanation));
        FeedbackResult result = feedback.feedback(clean, condition);

        synchronized (this) {
            int attempt = log.countAttempts(participantId, task.id()) + 1;
            log.append(new StudyRecord(
                    clock.instant().toString(),
                    participantId,
                    task.id(),
                    task.topic(),
                    attempt,
                    condition,
                    modelLabel,
                    clean.code(),
                    clean.explanation(),
                    result.feedback(),
                    result.matches()));
            return new StudyResponse(result.feedback(), attempt);
        }
    }
}
