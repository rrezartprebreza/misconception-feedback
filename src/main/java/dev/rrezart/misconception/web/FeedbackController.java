package dev.rrezart.misconception.web;

import dev.rrezart.misconception.language.LanguageRegistry;
import dev.rrezart.misconception.model.FeedbackCondition;
import dev.rrezart.misconception.model.FeedbackResult;
import dev.rrezart.misconception.model.Submission;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final LanguageRegistry registry;

    public FeedbackController(LanguageRegistry registry) {
        this.registry = registry;
    }

    @PostMapping
    public FeedbackResult feedback(@RequestBody FeedbackRequest request) {
        if (request.code() == null || request.code().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code is required");
        }
        if (request.condition() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "condition must be GENERIC or INFORMED");
        }
        Submission submission = new Submission(request.id(), request.task(), request.code(), request.explanation());
        return Languages.resolve(registry, request.language()).feedback().feedback(submission, request.condition());
    }
}
