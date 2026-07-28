package com.selectcar.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A homepage to scan. {@code type} distinguishes a car brand site from a news site;
 * it is informational only and does not change how the source is scanned.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Source(String name, String url, SourceType type) {

    public enum SourceType {
        BRAND,
        NEWS
    }
}
