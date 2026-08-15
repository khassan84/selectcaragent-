package com.selectcar.agent.agent;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.selectcar.agent.config.PipelineProperties;
import com.selectcar.agent.model.ExpertReview;
import com.selectcar.agent.model.ExpertReviewRequest;

public class ExpertReviewAgent2 {

    private static final Logger log = LoggerFactory.getLogger(ExpertReviewAgent2.class);

    private static final String SYSTEM = """
            You are a senior automotive journalist who has road-tested cars for over 20 years.
            Write balanced, hands-on expert reviews in clear English: praise what is good and be
            explicit about the weak points.
            Only use figures that are given to you in the prompt; never invent prices,
            performance numbers, dimensions or equipment. If a fact is not supplied, describe it
            qualitatively instead of guessing.
            Return only the review body, with no preamble and no word-count notes.
            """;
    
    
    private static final String SAMPLE_JSON = """
			{
				"brand": "Toyota",
				"model": "Corolla",
				"modelYear": 2025,
				"variant": "GR Sport 1.8 Hybrid",
				"market": "UK",
				"price": "from £30,995 OTR",
				"specs": [
					"0-100 km/h: 7.4 s",
					"Top speed: 180 km/h",
					"Fuel economy: 4.5 l/100 km"
				],
				"focusAreas": [
					"ride comfort",
					"interior quality"
				],
				"notes": "This is the latest generation of the Corolla, featuring a new hybrid powertrain and updated styling."
			}
			""";

    private final ChatClient chatClient;
    private final PipelineProperties properties;
    private final ObjectMapper objectMapper;

    
    public ExpertReviewAgent2(ChatClient chatClient, PipelineProperties properties, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        
    }

    /** Generates the review for {@code request} and returns it with metadata. */
    public String review(ExpertReviewRequest request) {
    	
    	   log.info("Generating expert review for {} {}", request.brand(), request.model());

           String userPrompt = """
                   Generate a detailed expert review for this car:

                   Brand: %s
                   Model: %s

                   Follow this exact JSON structure (use it as reference):

                   %s
   :
   				Structure the review with these sections:
   				1. Introduction – positioning, target audience and overall character
                   2. Design & Exterior – looks, build quality, dimensions
                   3. Interior & Features – space, materials, technology, comfort
                   4. Engine, Performance & Driving Experience – powertrain, handling, ride quality
                   5. Fuel Efficiency & Practicality
                   6. Pros and Cons
                   7. Final Verdict – who should buy it and why

                   Rules:
                   - Return ONLY valid JSON matching the sample structure
                   - overall_rating must be a number between 1.0 and 10.0
                   - Write in clear, professional English
                   - Be balanced and realistic
                   - Do not invent specific numbers you are not sure about
                   """.formatted(request.brand(), request.model(), SAMPLE_JSON);
           
           String jsonResponse = chatClient.prompt()
                   .system(SYSTEM)
                   .user(userPrompt)
                   .call()
                   .content();
           
           try {
               // Clean possible markdown if model still adds it
               jsonResponse = cleanJson(jsonResponse);

               ExpertReview2 review = objectMapper.readValue(jsonResponse, ExpertReview2.class);

               // Force brand & model from input (in case model changes them)

               log.info("Successfully generated expert review for {} {} | Rating: {}",
            		   request.brand(), request.model(), review.getOverallRating());

               return jsonResponse;

           } catch (Exception e) {
               log.error("Failed to parse expert review JSON for {} {}", request.brand(), request.model(), e);
               log.error("Raw response was:\n{}", jsonResponse);
               throw new RuntimeException("Failed to generate expert review for " + request.brand() + " " + request.model(), e);
           }
       
    }
    
    
    private String cleanJson(String text) {
        if (text == null) return "{}";
        text = text.trim();
        // Remove markdown code blocks if present
        if (text.startsWith("```")) {
            text = text.replaceAll("(?s)^```(?:json)?\\s*", "")
                       .replaceAll("\\s*```$", "");
        }
        return text.trim();
    }

    public String buildPrompt(ExpertReviewRequest request, int minWords) {
        StringBuilder prompt = new StringBuilder("""
                Write an expert review of at least %d words for the %s.

                """.formatted(minWords, request.displayName()));

        prompt.append("Known details:\n");
        appendIfPresent(prompt, "Brand", request.brand());
        appendIfPresent(prompt, "Model", request.model());
        appendIfPresent(prompt, "Model year", request.modelYear() == null ? null : request.modelYear().toString());
        appendIfPresent(prompt, "Variant/trim", request.variant());
        appendIfPresent(prompt, "Market", request.market());
        appendIfPresent(prompt, "Price", request.price());
        appendList(prompt, "Specifications", request.specs());
        appendList(prompt, "Aspects to focus on", request.focusAreas());
        appendIfPresent(prompt, "Additional notes", request.notes());

        prompt.append("""

                Structure the review with these sections, each as a short heading followed by prose:
                Verdict summary, Design and interior, Driving and performance,
                Practicality and technology, Running costs and value, Rivals to consider,
                Pros and cons (as two short bullet lists), Who should buy it.
                Close with a rating out of 10 and one sentence justifying it.
                """);
        return prompt.toString();
    }

    public int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private void validate(ExpertReviewRequest request) {
        if (request == null || isBlank(request.brand()) || isBlank(request.model())) {
            throw new IllegalArgumentException("Expert review requires both a brand and a model");
        }
    }

    private void appendIfPresent(StringBuilder prompt, String label, String value) {
        if (!isBlank(value)) {
            prompt.append("- ").append(label).append(": ").append(value.trim()).append('\n');
        }
    }

    private void appendList(StringBuilder prompt, String label, List<String> values) {
        List<String> present = values.stream().filter(v -> !isBlank(v)).map(String::trim).toList();
        if (present.isEmpty()) {
            return;
        }
        prompt.append("- ").append(label).append(":\n");
        present.forEach(value -> prompt.append("  * ").append(value).append('\n'));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String chat(String userPrompt) {
        return chatClient.prompt().system(SYSTEM).user(userPrompt).call().content();
    }

    
    
    public static class ExpertReview2 {
        private String brand;
        private String model;
        private String introduction;
        private String designExterior;
        private String interiorFeatures;
        private String performanceDriving;
        private String fuelEfficiencyPracticality;
        private java.util.List<String> pros;
        private java.util.List<String> cons;
        private String verdict;
        private double overallRating;

        // Getters & Setters
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public String getIntroduction() { return introduction; }
        public void setIntroduction(String introduction) { this.introduction = introduction; }

        public String getDesignExterior() { return designExterior; }
        public void setDesignExterior(String designExterior) { this.designExterior = designExterior; }

        public String getInteriorFeatures() { return interiorFeatures; }
        public void setInteriorFeatures(String interiorFeatures) { this.interiorFeatures = interiorFeatures; }

        public String getPerformanceDriving() { return performanceDriving; }
        public void setPerformanceDriving(String performanceDriving) { this.performanceDriving = performanceDriving; }

        public String getFuelEfficiencyPracticality() { return fuelEfficiencyPracticality; }
        public void setFuelEfficiencyPracticality(String fuelEfficiencyPracticality) {
            this.fuelEfficiencyPracticality = fuelEfficiencyPracticality;
        }

        public java.util.List<String> getPros() { return pros; }
        public void setPros(java.util.List<String> pros) { this.pros = pros; }

        public java.util.List<String> getCons() { return cons; }
        public void setCons(java.util.List<String> cons) { this.cons = cons; }

        public String getVerdict() { return verdict; }
        public void setVerdict(String verdict) { this.verdict = verdict; }

        public double getOverallRating() { return overallRating; }
        public void setOverallRating(double overallRating) { this.overallRating = overallRating; }
    }
    

}
