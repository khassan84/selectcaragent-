package com.selectcar.agent.agent;

import com.selectcar.agent.config.PipelineProperties;
import com.selectcar.agent.model.ExpertReview;
import com.selectcar.agent.model.ExpertReviewRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Agent 9 — writes an expert review for one particular model of a brand. Any facts the caller
 * knows (model year, variant, market, price, specs, focus areas, free-form notes) are passed
 * through {@link ExpertReviewRequest} and injected into the prompt; missing fields are omitted
 * so the model is never asked to comment on information it does not have.
 */
@Component
public class ExpertReviewAgent {

    private static final Logger log = LoggerFactory.getLogger(ExpertReviewAgent.class);

    private static final String SYSTEM = """
            You are a senior automotive journalist who has road-tested cars for over 20 years.
            Write balanced, hands-on expert reviews in clear English: praise what is good and be
            explicit about the weak points.
            Only use figures that are given to you in the prompt; never invent prices,
            performance numbers, dimensions or equipment. If a fact is not supplied, describe it
            qualitatively instead of guessing.
            Return only the review body, with no preamble and no word-count notes.
            """;

    private final ChatClient chatClient;
    private final PipelineProperties properties;

    public ExpertReviewAgent(ChatClient chatClient, PipelineProperties properties) {
        this.chatClient = chatClient;
        this.properties = properties;
    }

    /** Generates the review for {@code request} and returns it with metadata. */
    public ExpertReview review(ExpertReviewRequest request) {
        validate(request);
        int minWords = properties.getMinReviewWords();

        String body = chat(buildPrompt(request, minWords));
        if (wordCount(body) < minWords) {
            log.info("Review draft for '{}' was {} words; requesting expansion",
                    request.displayName(), wordCount(body));
            body = chat("""
                    The following car review is too short. Expand it to at least %d words with more
                    detail and analysis, keeping every existing fact and adding no new figures.
                    Return only the expanded review body.

                    %s
                    """.formatted(minWords, body));
        }

        log.info("Generated expert review for '{}' ({} words)", request.displayName(), wordCount(body));
        return new ExpertReview(
                request.brand(),
                request.model(),
                request.modelYear(),
                request.variant(),
                request.market(),
                "%s review".formatted(request.displayName()),
                body,
                wordCount(body),
                Instant.now());
    }

    public String buildPrompt(ExpertReviewRequest request, int minWords) {
        StringBuilder prompt = new StringBuilder("""
                Write an expert review of at least %d words for the %s.

                """.formatted(minWords, request.displayName()));

        prompt.append("Known details:\n");
        appendIfPresent(prompt, "Brand", request.brand());
        appendIfPresent(prompt, "Model", request.model());
        appendIfPresent(prompt, "Model year", request.modelYear() == null ? null : request.modelYear().toString());
        appendIfPresent(prompt, "Variant/trim", request.variant());
        appendIfPresent(prompt, "Market", request.market());
        appendIfPresent(prompt, "Price", request.price());
        appendList(prompt, "Specifications", request.specs());
        appendList(prompt, "Aspects to focus on", request.focusAreas());
        appendIfPresent(prompt, "Additional notes", request.notes());

        prompt.append("""

                Structure the review with these sections, each as a short heading followed by prose:
                Verdict summary, Design and interior, Driving and performance,
                Practicality and technology, Running costs and value, Rivals to consider,
                Pros and cons (as two short bullet lists), Who should buy it.
                Close with a rating out of 10 and one sentence justifying it.
                """);
        return prompt.toString();
    }

    public int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private void validate(ExpertReviewRequest request) {
        if (request == null || isBlank(request.brand()) || isBlank(request.model())) {
            throw new IllegalArgumentException("Expert review requires both a brand and a model");
        }
    }

    private void appendIfPresent(StringBuilder prompt, String label, String value) {
        if (!isBlank(value)) {
            prompt.append("- ").append(label).append(": ").append(value.trim()).append('\n');
        }
    }

    private void appendList(StringBuilder prompt, String label, List<String> values) {
        List<String> present = values.stream().filter(v -> !isBlank(v)).map(String::trim).toList();
        if (present.isEmpty()) {
            return;
        }
        prompt.append("- ").append(label).append(":\n");
        present.forEach(value -> prompt.append("  * ").append(value).append('\n'));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String chat(String userPrompt) {
        return chatClient.prompt().system(SYSTEM).user(userPrompt).call().content();
    }
}
