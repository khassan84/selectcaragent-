package com.selectcar.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * An entry of {@code expertreviews.json}: the expert review generated for one brand/model.
 *
 * @param brand       manufacturer the review is about
 * @param model       model the review is about
 * @param modelYear   model year, may be {@code null}
 * @param variant     trim/variant, may be {@code null}
 * @param market      market/region the review targets, may be {@code null}
 * @param title       headline of the review
 * @param review      full review body
 * @param wordCount   number of words in {@code review}
 * @param generatedAt generation timestamp
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpertReview(
        String brand,
        String model,
        Integer modelYear,
        String variant,
        String market,
        String title,
        String review,
        int wordCount,
        Instant generatedAt) {
}
