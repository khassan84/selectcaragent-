package com.selectcar.agent.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = false)
public class Variant {

    @NotBlank
    @JsonProperty("variantName")
    private String variantName;

    @JsonProperty("price")
    private String price;

    @JsonProperty("image")
    private String image;

    @JsonProperty("info")
    private List<VariantInfo> info;

    // Getters and Setters

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<VariantInfo> getInfo() {
        return info;
    }

    public void setInfo(List<VariantInfo> info) {
        this.info = info;
    }
}
