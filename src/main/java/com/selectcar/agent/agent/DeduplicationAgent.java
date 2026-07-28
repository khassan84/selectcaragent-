package com.selectcar.agent.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.selectcar.agent.config.PipelineProperties;
import com.selectcar.agent.model.ScannedLink;
import com.selectcar.agent.util.JsonStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Agent 4 — filters out links already recorded in {@code processed.json} and persists newly
 * processed URLs so subsequent runs skip them.
 */
@Component
public class DeduplicationAgent {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationAgent.class);

    private final JsonStore jsonStore;
    private final PipelineProperties properties;

    public DeduplicationAgent(JsonStore jsonStore, PipelineProperties properties) {
        this.jsonStore = jsonStore;
        this.properties = properties;
    }

    public Set<String> loadProcessed() {
        String file = properties.getProcessedFile();
        if (!jsonStore.exists(file)) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(jsonStore.read(file, new TypeReference<List<String>>() {
        }));
    }

    public List<ScannedLink> removeProcessed(List<ScannedLink> links) {
        Set<String> processed = loadProcessed();
        List<ScannedLink> fresh = links.stream()
                .filter(link -> !processed.contains(link.url()))
                .toList();
        log.info("Deduplication: {} of {} link(s) are new", fresh.size(), links.size());
        return fresh;
    }

    public void markProcessed(List<String> urls) {
        Set<String> processed = loadProcessed();
        processed.addAll(urls);
        jsonStore.write(properties.getProcessedFile(), new ArrayList<>(processed));
        log.info("Recorded {} URL(s) as processed (total {})", urls.size(), processed.size());
    }
}
