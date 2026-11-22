package dev.rrezart.misconception.study;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudyTasksTest {

    @Test
    void bundledTasksCoverBothTopics() {
        StudyTasks tasks = StudyTasks.fromClasspath(new ObjectMapper(), "/study/tasks.json");

        assertThat(tasks.all()).extracting(StudyTask::topic).contains(1, 2);
        assertThat(tasks.find("binary-search")).isPresent();
        assertThat(tasks.find("missing")).isEmpty();
    }

    @Test
    void rejectsDuplicateIdsAndBadTopics() {
        StudyTask a = new StudyTask("a", 1, "A", "text");
        assertThatThrownBy(() -> new StudyTasks(List.of(a, new StudyTask("a", 2, "B", "text"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StudyTasks(List.of(new StudyTask("c", 3, "C", "text"))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
