package com.selectcar.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Final output produced by the Ollama-backed generation agents for a single source link.
 *
 * @param sourceUrl        original article URL the generation was based on
 * @param sourceName       source the link came from
 * @param title            headline for the generated piece
 * @param article          full news article (>= configured minimum word count)
 * @param wordCount        number of words in {@code article}
 * @param socialShortStory short social-media story/caption
 * @param videoScript      short-form video generation script
 * @param generatedAt      generation timestamp
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedArticle(
        String sourceUrl,
        String sourceName,
        String title,
        String article,
        int wordCount,
        String socialShortStory,
        String videoScript,
        Instant generatedAt) {
}
