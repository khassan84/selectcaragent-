package com.selectcar.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * An entry of {@code articlelist.json}: an extracted title + summary for a link that
 * is car-related, recent and not previously processed.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArticleListItem(
        String url,
        String sourceName,
        String title,
        String summary,
        Instant publishedAt) {
}
