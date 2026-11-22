package dev.rrezart.misconception.web;

import dev.rrezart.misconception.collect.CollectedAnswer;
import dev.rrezart.misconception.collect.CollectionService;
import dev.rrezart.misconception.eval.GoldItem;
import dev.rrezart.misconception.eval.PilotEvaluator;
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

    private final CollectionService service;
    private final String adminToken;

    public CollectController(CollectionService service, @Value("${collect.admin-token:}") String adminToken) {
        this.service = service;
        this.adminToken = adminToken == null ? "" : adminToken;
    }

    @PostMapping("/submit")
    public Map<String, Object> submit(@RequestBody CollectRequest request) {
        if (!request.consent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "consent is required");
        }
        try {
            service.submit(request.participantId(), request.taskId(), request.code(), request.explanation());
            return Map.of("received", true);
        } catch (UnknownTaskException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/admin/answers")
    public List<AdminAnswerView> answers(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                         @RequestParam String labeller) {
        requireAdmin(token);
        return service.store().all().stream()
                .map(a -> view(a, labeller))
                .toList();
    }

    @PutMapping("/admin/labels")
    public Map<String, Object> label(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                     @RequestBody LabelRequest request) {
        requireAdmin(token);
        try {
            service.label(request.labeller(), request.answerId(), request.labels());
            return Map.of("saved", true);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/admin/export")
    public List<GoldItem> export(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                 @RequestParam String labeller) {
        requireAdmin(token);
        return service.exportGold(labeller);
    }

    @GetMapping("/admin/agreement")
    public PilotEvaluator.Report agreement(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                           @RequestParam String a,
                                           @RequestParam String b) {
        requireAdmin(token);
        return service.agreement(a, b);
    }

    private AdminAnswerView view(CollectedAnswer a, String labeller) {
        List<String> labels = service.store().labelsOf(labeller, a.id()).orElse(null);
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
