package com.selectcar.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Input for {@link com.selectcar.agent.agent.ExpertReviewAgent}: the car to review plus any
 * optional facts the caller can supply. Only {@code brand} and {@code model} are required —
 * every other field may be {@code null} or empty and is then simply left out of the prompt.
 *
 * @param brand      manufacturer, e.g. {@code "Toyota"}
 * @param model      model name, e.g. {@code "Corolla"}
 * @param modelYear  model year, e.g. {@code 2025}
 * @param variant    trim/variant or engine, e.g. {@code "GR Sport 1.8 Hybrid"}
 * @param market     market/region the review targets, e.g. {@code "UK"}
 * @param price      price information as free text, e.g. {@code "from £30,995 OTR"}
 * @param specs      known specifications, one fact per entry, e.g. {@code "0-100 km/h: 7.4 s"}
 * @param focusAreas aspects the review should emphasise, e.g. {@code "ride comfort"}
 * @param notes      any additional context the caller wants the reviewer to use
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpertReviewRequest(
        String brand,
        String model,
        Integer modelYear,
        String variant,
        String market,
        String price,
        List<String> specs,
        List<String> focusAreas,
        String notes) {

    public ExpertReviewRequest {
        specs = specs == null ? List.of() : List.copyOf(specs);
        focusAreas = focusAreas == null ? List.of() : List.copyOf(focusAreas);
    }

    public static ExpertReviewRequest of(String brand, String model) {
        return new ExpertReviewRequest(brand, model, null, null, null, null, List.of(), List.of(), null);
    }
    
    public static ExpertReviewRequest of(String brand, String model, String price, String variant) {
        return new ExpertReviewRequest(brand, model, null, variant, null, price, List.of(), List.of(), null);
    }

    /** Human-readable name of the car, e.g. {@code "2025 Toyota Corolla GR Sport"}. */
    public String displayName() {
        StringBuilder sb = new StringBuilder();
        if (modelYear != null) {
            sb.append(modelYear).append(' ');
        }
        sb.append(brand == null ? "" : brand.trim());
        if (model != null && !model.isBlank()) {
            sb.append(' ').append(model.trim());
        }
        if (variant != null && !variant.isBlank()) {
            sb.append(' ').append(variant.trim());
        }
        return sb.toString().trim();
    }
}
