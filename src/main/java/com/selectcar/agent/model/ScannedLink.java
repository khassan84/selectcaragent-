package com.selectcar.agent.model;

import java.time.Instant;

/**
 * A candidate article link discovered while scanning a source homepage.
 *
 * @param url           absolute URL of the article
 * @param sourceName    name of the source it was found on
 * @param anchorText    visible link text (used for early car-relevance heuristics)
 * @param publishedAt   best-effort publication timestamp, or {@code null} if unknown
 */
public record ScannedLink(String url, String sourceName, String anchorText, Instant publishedAt) {

    public ScannedLink withPublishedAt(Instant when) {
        return new ScannedLink(url, sourceName, anchorText, when);
    }
}
