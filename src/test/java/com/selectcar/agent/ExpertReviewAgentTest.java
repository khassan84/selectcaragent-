package com.selectcar.agent;

import com.selectcar.agent.agent.ExpertReviewAgent;
import com.selectcar.agent.config.PipelineProperties;
import com.selectcar.agent.model.ExpertReviewRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpertReviewAgentTest {

    private final ExpertReviewAgent agent = new ExpertReviewAgent(null, new PipelineProperties());

    @Test
    void promptIncludesEverySuppliedDetail() {
        ExpertReviewRequest request = new ExpertReviewRequest(
                "Toyota", "Corolla", 2025, "GR Sport", "UK", "from £30,995",
                List.of("140 kW hybrid", "0-100 km/h: 7.4 s"),
                List.of("ride comfort"),
                "Mid-life facelift");

        String prompt = agent.buildPrompt(request, 1000);

        assertThat(prompt).contains("at least 1000 words", "2025 Toyota Corolla GR Sport")
                .contains("Market: UK", "Price: from £30,995", "140 kW hybrid", "ride comfort",
                        "Mid-life facelift");
    }

    @Test
    void promptOmitsMissingOptionalDetails() {
        String prompt = agent.buildPrompt(ExpertReviewRequest.of("Kia", "EV6"), 800);

        assertThat(prompt).contains("Kia EV6").doesNotContain("Market:", "Price:", "Variant/trim:",
                "Specifications:", "Additional notes:");
    }

    @Test
    void requiresBrandAndModel() {
        assertThatThrownBy(() -> agent.review(new ExpertReviewRequest(
                "Toyota", " ", null, null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void countsWords() {
        assertThat(agent.wordCount("  a very short review ")).isEqualTo(4);
        assertThat(agent.wordCount(null)).isZero();
    }
}
