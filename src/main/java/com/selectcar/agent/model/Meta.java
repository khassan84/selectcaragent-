package com.selectcar.agent.model;

import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = false)
    public class Meta {

        @NotBlank
        @JsonProperty("variant_name")
        private String variantName;

        @NotBlank
        @JsonProperty("brand")
        private String brand;

        @NotBlank
        @JsonProperty("model")
        private String model;

        @NotBlank
        @Pattern(regexp = "^₹[0-9,]+$")
        @JsonProperty("price")
        private String price;

        @NotBlank
        @JsonProperty("fuel")
        private String fuel;

        @NotBlank
        @JsonProperty("transmission")
        private String transmission;

        @NotBlank
        @JsonProperty("drive")
        private String drive;

        @NotNull
        @Min(1)
        @JsonProperty("seating")
        private Integer seating;

        @JsonProperty("badge")
        private String badge; // nullable allowed

        @NotBlank
        @JsonProperty("image")
        private String image;

        @NotNull
        @Size(min = 1)
        @JsonProperty("highlights")
        private List<@NotBlank String> highlights;

        // Getters and Setters

        public String getVariantName() {
            return variantName;
        }

        public void setVariantName(String variantName) {
            this.variantName = variantName;
        }

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

        public String getPrice() {
            return price;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public String getFuel() {
            return fuel;
        }

        public void setFuel(String fuel) {
            this.fuel = fuel;
        }

        public String getTransmission() {
            return transmission;
        }

        public void setTransmission(String transmission) {
            this.transmission = transmission;
        }

        public String getDrive() {
            return drive;
        }

        public void setDrive(String drive) {
            this.drive = drive;
        }

        public Integer getSeating() {
            return seating;
        }

        public void setSeating(Integer seating) {
            this.seating = seating;
        }

        public String getBadge() {
            return badge;
        }

        public void setBadge(String badge) {
            this.badge = badge;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public List<String> getHighlights() {
            return highlights;
        }

        public void setHighlights(List<String> highlights) {
            this.highlights = highlights;
        }
    }
