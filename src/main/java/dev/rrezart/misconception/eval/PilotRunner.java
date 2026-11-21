package dev.rrezart.misconception.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rrezart.misconception.catalogue.MisconceptionCatalogue;
import dev.rrezart.misconception.feedback.FeedbackService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Pilot run: sends a gold-labelled set through the detector and prints agreement metrics.
 * Start with the "pilot" profile. By default it uses the synthetic set bundled in the jar; set
 * study.pilot.gold-file to a path to use an instructor-labelled file instead.
 */
@Component
@Profile("pilot")
public class PilotRunner implements CommandLineRunner {

    private final FeedbackService service;
    private final MisconceptionCatalogue catalogue;
    private final ObjectMapper mapper;
    private final String goldFile;

    public PilotRunner(FeedbackService service,
                       MisconceptionCatalogue catalogue,
                       ObjectMapper mapper,
                       @Value("${study.pilot.gold-file:}") String goldFile) {
        this.service = service;
        this.catalogue = catalogue;
        this.mapper = mapper;
        this.goldFile = goldFile;
    }

    @Override
    public void run(String... args) throws IOException {
        List<GoldItem> items = load();
        System.out.println("Running detector on " + items.size() + " items...");
        PilotEvaluator.Report report = PilotEvaluator.evaluate(items, service::detectOnly, catalogue);
        System.out.println(PilotEvaluator.format(report));
    }

    private List<GoldItem> load() throws IOException {
        TypeReference<List<GoldItem>> type = new TypeReference<>() {
        };
        if (goldFile != null && !goldFile.isBlank()) {
            return mapper.readValue(Files.readString(Path.of(goldFile)), type);
        }
        try (InputStream in = PilotRunner.class.getResourceAsStream("/pilot/synthetic-gold.json")) {
            if (in == null) {
                throw new IOException("Bundled synthetic gold set not found");
            }
            return mapper.readValue(in, type);
        }
    }
}
