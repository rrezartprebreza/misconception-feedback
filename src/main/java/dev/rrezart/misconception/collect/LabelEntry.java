package dev.rrezart.misconception.collect;

import java.util.List;

/** One labelling decision. An empty list means "none of the catalogue misconceptions". */
public record LabelEntry(String id, String labeller, List<String> labels, String at) {
}
