package com.selectcar.agent.agent;

import com.selectcar.agent.model.ScannedLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Agent 3 — removes links that are not car-related, using keyword heuristics over the link
 * URL and its anchor text. Cheap and offline, so it runs before any page is fetched.
 */
@Component
public class CarRelevanceFilterAgent {

    private static final Logger log = LoggerFactory.getLogger(CarRelevanceFilterAgent.class);

    private static final Set<String> CAR_KEYWORDS = Set.of(
            "car", "cars", "auto", "autos", "automobile", "automotive", "vehicle", "vehicles",
            "suv", "sedan", "hatchback", "coupe", "truck", "pickup", "crossover",
            "ev", "electric-vehicle", "electric car", "hybrid", "engine", "horsepower",
            "drivetrain", "mpg", "test-drive", "test drive", "review", "spy-shot", "spy shots",
            "model", "trim", "facelift", "concept-car", "supercar", "hypercar", "roadster");

    public List<ScannedLink> filter(List<ScannedLink> links) {
        List<ScannedLink> kept = links.stream().filter(this::isCarRelated).toList();
        log.info("Car-relevance filter: kept {} of {} link(s)", kept.size(), links.size());
        return kept;
    }

    public boolean isCarRelated(ScannedLink link) {
        String haystack = ((link.anchorText() == null ? "" : link.anchorText()) + " " + link.url())
                .toLowerCase();
        for (String keyword : CAR_KEYWORDS) {
            if (haystack.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
