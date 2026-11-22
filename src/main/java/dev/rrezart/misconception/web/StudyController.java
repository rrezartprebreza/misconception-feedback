package dev.rrezart.misconception.web;

import dev.rrezart.misconception.study.StudyResponse;
import dev.rrezart.misconception.study.StudyService;
import dev.rrezart.misconception.study.StudyTask;
import dev.rrezart.misconception.study.StudyTasks;
import dev.rrezart.misconception.study.UnknownTaskException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/study")
public class StudyController {

    private final StudyService study;
    private final StudyTasks tasks;

    public StudyController(StudyService study, StudyTasks tasks) {
        this.study = study;
        this.tasks = tasks;
    }

    @GetMapping("/tasks")
    public List<StudyTask> tasks() {
        return tasks.all();
    }

    @PostMapping("/submit")
    public StudyResponse submit(@RequestBody StudyRequest request) {
        if (!request.consent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "consent is required");
        }
        try {
            return study.submit(request.participantId(), request.taskId(), request.code(), request.explanation());
        } catch (UnknownTaskException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
