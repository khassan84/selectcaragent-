package com.selectcar.agent.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Model {

    @NotBlank
    @JsonProperty("brand")
    private String brand;

    @NotBlank
    @JsonProperty("model")
    private String model;

    @NotBlank
    @JsonProperty("slug")
    private String slug;

    @JsonProperty("thumb")
    private String thumb;

    @JsonProperty("officialVideo")
    private String officialVideo;

    @JsonProperty("videoCourtesy")
    private String videoCourtesy;

    @NotBlank
    @JsonProperty("summary")
    private String summary;
    
    @JsonProperty("brandSlug")
    private String brandSlug;

    @JsonProperty("images")
    private List<String> images;
    
    private List<Variant> variants;

    // Getters and Setters

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public String getOfficialVideo() {
        return officialVideo;
    }

    public void setOfficialVideo(String officialVideo) {
        this.officialVideo = officialVideo;
    }

    public String getVideoCourtesy() {
        return videoCourtesy;
    }

    public void setVideoCourtesy(String videoCourtesy) {
        this.videoCourtesy = videoCourtesy;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

	/**
	 * @return the variants
	 */
	public List<Variant> getVariants() {
		return variants;
	}

	/**
	 * @param variants the variants to set
	 */
	public void setVariants(List<Variant> variants) {
		this.variants = variants;
	}

	public String getBrandSlug() {
		return brandSlug;
	}

	public void setBrandSlug(String brandSlug) {
		this.brandSlug = brandSlug;
	}
	
	
}
