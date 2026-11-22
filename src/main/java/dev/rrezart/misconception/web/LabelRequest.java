package dev.rrezart.misconception.web;

import java.util.List;

public record LabelRequest(String labeller, String answerId, List<String> labels) {
}
