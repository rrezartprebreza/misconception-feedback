package dev.rrezart.misconception.detect;

import dev.rrezart.misconception.model.Match;
import dev.rrezart.misconception.model.Submission;

import java.util.List;

public interface Detector {

    List<Match> detect(Submission submission);
}
