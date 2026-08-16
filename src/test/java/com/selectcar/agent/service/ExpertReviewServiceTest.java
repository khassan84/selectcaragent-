package com.selectcar.agent.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selectcar.agent.config.PipelineProperties;
import com.selectcar.agent.model.Model;
import com.selectcar.agent.model.ReviewStatus;
import com.selectcar.agent.util.JsonStore;

import static org.assertj.core.api.Assertions.assertThat;

class ExpertReviewServiceTest {

    private static final String REVIEW_JSON = """
            {"brand":"maruti","model":"brezza","reviews":[{"header":"Verdict","text":"Solid."}]}
            """;

    @TempDir
    Path dataDir;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PipelineProperties properties = new PipelineProperties();
    private final ExpertReviewService service = new ExpertReviewService();

    @BeforeEach
    void setUp() {
        properties.setDataDir(dataDir.toString());
        service.properties = properties;
        service.jsonStore = new JsonStore(objectMapper, properties);
    }

    @Test
    void savesReviewAsOneFilePerModel() throws Exception {
        String file = service.saveReview(model("Maruti Suzuki", "Brezza", "maruti", "brezza"), REVIEW_JSON);

        assertThat(file).isEqualTo("expertreviews/maruti-brezza.json");
        assertThat(objectMapper.readTree(Files.readString(dataDir.resolve(file))).path("reviews"))
                .hasSize(1);
    }

    @Test
    void fallsBackToBrandAndModelNamesWhenSlugsAreMissing() {
        String file = service.saveReview(model("Maruti Suzuki", "Grand Vitara", null, null), REVIEW_JSON);

        assertThat(file).isEqualTo("expertreviews/maruti-suzuki-grand-vitara.json");
    }

    @Test
    void addsReviewStatusEntryWhenFileIsMissing() {
        service.markReviewDone(model("Kia", "EV6", "kia", "ev6"));

        assertThat(readStatus()).singleElement()
                .satisfies(status -> {
                    assertThat(status.getBrand()).isEqualTo("Kia");
                    assertThat(status.getModel()).isEqualTo("EV6");
                    assertThat(status.getStatus()).isEqualTo("done");
                });
    }

    @Test
    void updatesExistingReviewStatusEntryInsteadOfDuplicatingIt() {
        service.jsonStore.writeString(properties.getReviewStatus(),
                """
                [{"brand":"kia","model":"ev6","status":"pending"}]
                """);

        service.markReviewDone(model("Kia", "EV6", "kia", "ev6"));

        assertThat(readStatus()).singleElement()
                .satisfies(status -> assertThat(status.getStatus()).isEqualTo("done"));
    }

    private List<ReviewStatus> readStatus() {
        return service.jsonStore.read(properties.getReviewStatus(), new TypeReference<List<ReviewStatus>>() {
        });
    }

    private Model model(String brand, String modelName, String brandSlug, String slug) {
        Model model = new Model();
        model.setBrand(brand);
        model.setModel(modelName);
        model.setBrandSlug(brandSlug);
        model.setSlug(slug);
        return model;
    }
}
