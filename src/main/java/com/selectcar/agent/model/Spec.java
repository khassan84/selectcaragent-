package com.selectcar.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = false)
    public class Spec {

        @NotBlank
        @JsonProperty("group")
        private String group;

        @NotBlank
        @JsonProperty("label")
        private String label;

        @NotBlank
        @JsonProperty("value")
        private String value;

        // Getters and Setters

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
