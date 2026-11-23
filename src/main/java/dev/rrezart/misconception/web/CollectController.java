package dev.rrezart.misconception.web;

import dev.rrezart.misconception.collect.AnswerStore;
import dev.rrezart.misconception.collect.CollectedAnswer;
import dev.rrezart.misconception.collect.UnknownParticipantException;
import dev.rrezart.misconception.eval.GoldItem;
import dev.rrezart.misconception.eval.PilotEvaluator;
import dev.rrezart.misconception.language.LanguageContext;
import dev.rrezart.misconception.language.LanguageRegistry;
import dev.rrezart.misconception.study.UnknownTaskException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/**
 * Student submission is open to anyone with the link and consent. Everything under /admin needs the
 * instructor token (header X-Admin-Token) and is switched off when no token is configured.
 */
@RestController
@RequestMapping("/api/collect")
public class CollectController {

    private final LanguageRegistry registry;
    private final AnswerStore store;
    private final String adminToken;

    public CollectController(LanguageRegistry registry, AnswerStore store,
                             @Value("${collect.admin-token:}") String adminToken) {
        this.registry = registry;
        this.store = store;
        this.adminToken = adminToken == null ? "" : adminToken;
    }

    @PostMapping("/submit")
    public Map<String, Object> submit(@RequestBody CollectRequest request) {
        if (!request.consent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "consent is required");
        }
        LanguageContext context = Languages.resolve(registry, request.language());
        try {
            context.collection().submit(request.participantId(), request.taskId(), request.code(), request.explanation());
            return Map.of("received", true);
        } catch (UnknownTaskException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (UnknownParticipantException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "That ID is not on the list");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** Lets the page check an ID before the student writes anything. 403 means the ID is not on the list. */
    @GetMapping("/check")
    public Map<String, Object> check(@RequestParam String id, @RequestParam(required = false) String language) {
        LanguageContext context = Languages.resolve(registry, language);
        try {
            context.collection().checkParticipant(id);
            return Map.of("allowed", true);
        } catch (UnknownParticipantException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "That ID is not on the list");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/admin/answers")
    public List<AdminAnswerView> answers(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                         @RequestParam String labeller) {
        requireAdmin(token);
        return store.all().stream()
                .map(a -> view(a, labeller))
                .toList();
    }

    @PutMapping("/admin/labels")
    public Map<String, Object> label(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                     @RequestBody LabelRequest request) {
        requireAdmin(token);
        try {
            CollectedAnswer answer = store.find(request.answerId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown answer: " + request.answerId()));
            Languages.resolve(registry, answer.language()).collection()
                    .label(request.labeller(), request.answerId(), request.labels());
            return Map.of("saved", true);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/admin/export")
    public List<GoldItem> export(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                 @RequestParam String labeller,
                                 @RequestParam(required = false) String language) {
        requireAdmin(token);
        return Languages.resolve(registry, language).collection().exportGold(labeller);
    }

    @GetMapping("/admin/agreement")
    public PilotEvaluator.Report agreement(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                           @RequestParam String a,
                                           @RequestParam String b,
                                           @RequestParam(required = false) String language) {
        requireAdmin(token);
        return Languages.resolve(registry, language).collection().agreement(a, b);
    }

    private AdminAnswerView view(CollectedAnswer a, String labeller) {
        List<String> labels = store.labelsOf(labeller, a.id()).orElse(null);
        return new AdminAnswerView(a.id(), a.taskId(), a.task(), a.language(), a.code(), a.explanation(), labels);
    }

    private void requireAdmin(String token) {
        if (adminToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        boolean same = token != null && MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), adminToken.getBytes(StandardCharsets.UTF_8));
        if (!same) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "wrong or missing instructor token");
        }
    }
}
