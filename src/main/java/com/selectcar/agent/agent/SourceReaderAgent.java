package com.selectcar.agent.agent;

import com.selectcar.agent.config.PipelineProperties;
import com.selectcar.agent.model.Source;
import com.selectcar.agent.model.SourceCollection;
import com.selectcar.agent.util.JsonStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Agent 1 — loads the brand and news homepages from the local {@code website.json}.
 */
@Component
public class SourceReaderAgent {

    private static final Logger log = LoggerFactory.getLogger(SourceReaderAgent.class);

    private final JsonStore jsonStore;
    private final PipelineProperties properties;

    public SourceReaderAgent(JsonStore jsonStore, PipelineProperties properties) {
        this.jsonStore = jsonStore;
        this.properties = properties;
    }

    public List<Source> readSources() {
        String file = properties.getSourcesFile();
        if (!jsonStore.exists(file)) {
            throw new IllegalStateException("Sources file not found: " + jsonStore.resolve(file).toAbsolutePath()
                    + ". Create it with a { \"sources\": [ ... ] } structure.");
        }
        SourceCollection collection = jsonStore.read(file, SourceCollection.class);
        List<Source> sources = collection.sources();
        log.info("Loaded {} source homepage(s) from {}", sources.size(), file);
        return sources;
    }
}
