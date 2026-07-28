package com.selectcar.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Root object of {@code website.json}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SourceCollection(List<Source> sources) {

    public List<Source> sources() {
        return sources == null ? List.of() : sources;
    }
}
