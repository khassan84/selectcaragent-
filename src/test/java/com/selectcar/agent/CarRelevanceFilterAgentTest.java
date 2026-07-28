package com.selectcar.agent;

import com.selectcar.agent.agent.CarRelevanceFilterAgent;
import com.selectcar.agent.model.ScannedLink;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CarRelevanceFilterAgentTest {

    private final CarRelevanceFilterAgent agent = new CarRelevanceFilterAgent();

    @Test
    void keepsCarRelatedLinks() {
        ScannedLink carByText = new ScannedLink("https://example.com/a", "S", "New electric SUV revealed", null);
        ScannedLink carByUrl = new ScannedLink("https://example.com/cars/review", "S", "Read more", null);
        assertThat(agent.isCarRelated(carByText)).isTrue();
        assertThat(agent.isCarRelated(carByUrl)).isTrue();
    }

    @Test
    void dropsUnrelatedLinks() {
        ScannedLink unrelated = new ScannedLink("https://example.com/politics/election", "S", "Election results", null);
        assertThat(agent.isCarRelated(unrelated)).isFalse();
    }

    @Test
    void filterRemovesUnrelated() {
        List<ScannedLink> links = List.of(
                new ScannedLink("https://example.com/cars/1", "S", "SUV review", null),
                new ScannedLink("https://example.com/news/cooking", "S", "Best recipes", null));
        assertThat(agent.filter(links)).hasSize(1);
    }
}
