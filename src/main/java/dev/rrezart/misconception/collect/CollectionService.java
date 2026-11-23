package dev.rrezart.misconception.collect;

import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.eval.GoldItem;
import dev.rrezart.misconception.eval.PilotEvaluator;
import dev.rrezart.misconception.feedback.Anonymiser;
import dev.rrezart.misconception.model.Match;
import dev.rrezart.misconception.model.ProgrammingLanguage;
import dev.rrezart.misconception.model.Submission;
import dev.rrezart.misconception.study.StudyService;
import dev.rrezart.misconception.study.StudyTask;
import dev.rrezart.misconception.study.StudyTasks;
import dev.rrezart.misconception.study.UnknownTaskException;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Collects anonymised student answers for an instructor-labelled evaluation. No model is called and
 * the student sees no feedback, so collection does not change what students learn.
 */
public class CollectionService {

    static final int MAX_CODE_CHARS = 20_000;

    private final AnswerStore store;
    private final StudyTasks tasks;
    private final MisconceptionCatalogue catalogue;
    private final Anonymiser anonymiser;
    private final ProgrammingLanguage language;
    private final Clock clock;
    private final ParticipantIds participants;

    public CollectionService(AnswerStore store, StudyTasks tasks, MisconceptionCatalogue catalogue,
                             Anonymiser anonymiser, ProgrammingLanguage language, Clock clock) {
        this(store, tasks, catalogue, anonymiser, language, clock, ParticipantIds.open());
    }

    public CollectionService(AnswerStore store, StudyTasks tasks, MisconceptionCatalogue catalogue,
                             Anonymiser anonymiser, ProgrammingLanguage language, Clock clock,
                             ParticipantIds participants) {
        this.participants = participants;
        this.store = store;
        this.tasks = tasks;
        this.catalogue = catalogue;
        this.anonymiser = anonymiser;
        this.language = language;
        this.clock = clock;
    }

    /** Returns the ID as the instructor listed it. Throws when the ID is malformed or not on the list. */
    public String checkParticipant(String participantId) {
        if (!StudyService.isValidParticipantId(participantId)) {
            throw new IllegalArgumentException("Participant id must be 1 to 20 letters, digits, - or _");
        }
        return participants.canonical(participantId).orElseThrow(() -> new UnknownParticipantException(participantId));
    }

    public CollectedAnswer submit(String rawParticipantId, String taskId, String code, String explanation) {
        String participantId = checkParticipant(rawParticipantId);
        StudyTask task = tasks.find(taskId).orElseThrow(() -> new UnknownTaskException(taskId));
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (code.length() > MAX_CODE_CHARS || (explanation != null && explanation.length() > MAX_CODE_CHARS)) {
            throw new IllegalArgumentException("Answer is too long (limit " + MAX_CODE_CHARS + " characters)");
        }
        Submission clean = anonymiser.anonymise(new Submission(participantId + "/" + task.id(), task.text(), code, explanation));
        return store.add(clock.instant().toString(), participantId, task.id(), task.text(), language.id(),
                clean.code(), clean.explanation());
    }

    public void label(String labeller, String answerId, List<String> ids) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String id : ids == null ? List.<String>of() : ids) {
            if (catalogue.find(id).isEmpty()) {
                throw new IllegalArgumentException("Unknown misconception id: " + id);
            }
            unique.add(id);
        }
        store.setLabels(labeller, answerId, new ArrayList<>(unique), clock.instant().toString());
    }

    /** Only answers in this service's language that this labeller has labelled, in the format the pilot runner reads. */
    public List<GoldItem> exportGold(String labeller) {
        List<GoldItem> items = new ArrayList<>();
        for (CollectedAnswer answer : store.all()) {
            if (!language.id().equals(answer.language())) {
                continue;
            }
            store.labelsOf(labeller, answer.id()).ifPresent(labels ->
                    items.add(new GoldItem(answer.id(), answer.task(), answer.code(), answer.explanation(), labels)));
        }
        return items;
    }

    /** Agreement between two labellers on the answers both have labelled; labeller a is treated as the reference. */
    public PilotEvaluator.Report agreement(String a, String b) {
        List<GoldItem> both = new ArrayList<>();
        for (GoldItem item : exportGold(a)) {
            if (store.labelsOf(b, item.id()).isPresent()) {
                both.add(item);
            }
        }
        return PilotEvaluator.evaluate(both, submission -> store.labelsOf(b, submission.id()).orElse(List.of()).stream()
                .map(id -> new Match(id, "second labeller", 1.0)).toList(), catalogue);
    }

    public AnswerStore store() {
        return store;
    }
}
