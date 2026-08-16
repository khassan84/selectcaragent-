package com.selectcar.agent.agent;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selectcar.agent.config.PipelineProperties;
import com.selectcar.agent.util.JsonStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpertReviewAgent2Test {

    @TempDir
    Path dataDir;

    private ExpertReviewAgent2 agent(PipelineProperties properties) {
        properties.setDataDir(dataDir.toString());
        ObjectMapper objectMapper = new ObjectMapper();
        return new ExpertReviewAgent2(null, properties, objectMapper, new JsonStore(objectMapper, properties));
    }

    @Test
    void readsSampleFromDataDirectory() throws Exception {
        PipelineProperties properties = new PipelineProperties();
        Files.writeString(dataDir.resolve(properties.getExpertReviewSampleFile()),
                """
                {"brand":"maruti","reviews":[{"header":"Verdict","text":"Solid."}]}
                """);

        assertThat(agent(properties).sampleJson()).contains("\"reviews\"", "Verdict");
    }

    @Test
    void failsWhenSampleFileIsMissing() {
        assertThatThrownBy(() -> agent(new PipelineProperties()).sampleJson())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expertreviewsample.json");
    }
}
