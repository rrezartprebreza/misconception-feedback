package dev.rrezart.misconception.web;

import dev.rrezart.misconception.study.StudyResponse;
import dev.rrezart.misconception.language.LanguageContext;
import dev.rrezart.misconception.language.LanguageRegistry;
import dev.rrezart.misconception.study.StudyTask;
import dev.rrezart.misconception.study.UnknownTaskException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/study")
public class StudyController {

    private final LanguageRegistry registry;

    public StudyController(LanguageRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/tasks")
    public List<StudyTask> tasks(@RequestParam(required = false) String language) {
        return Languages.resolve(registry, language).tasks().all();
    }

    @PostMapping("/submit")
    public StudyResponse submit(@RequestBody StudyRequest request) {
        if (!request.consent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "consent is required");
        }
        LanguageContext context = Languages.resolve(registry, request.language());
        try {
            return context.study().submit(request.participantId(), request.taskId(), request.code(), request.explanation());
        } catch (UnknownTaskException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
