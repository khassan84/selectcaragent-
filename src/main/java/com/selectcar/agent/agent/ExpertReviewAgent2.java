package com.selectcar.agent.agent;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.selectcar.agent.config.PipelineProperties;
import com.selectcar.agent.model.ExpertReview;
import com.selectcar.agent.model.ExpertReviewRequest;
import com.selectcar.agent.util.JsonStore;

@Component
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

    private final ChatClient chatClient;
    private final PipelineProperties properties;
    private final ObjectMapper objectMapper;
    private final JsonStore jsonStore;

    /** Sample review JSON read from the data directory; cached after the first read. */
    private volatile String sampleJson;

    public ExpertReviewAgent2(ChatClient chatClient, PipelineProperties properties, ObjectMapper objectMapper,
            JsonStore jsonStore) {
        this.chatClient = chatClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.jsonStore = jsonStore;
    }

    /** Loads {@code expertreviewsample.json} from the data directory and caches it. */
    String sampleJson() {
        String cached = sampleJson;
        if (cached == null) {
            String file = properties.getExpertReviewSampleFile();
            if (!jsonStore.exists(file)) {
                throw new IllegalStateException("Expert review sample file not found: " + jsonStore.resolve(file));
            }
            cached = jsonStore.readString(file);
            sampleJson = cached;
        }
        return cached;
    }

    /** Generates the review for {@code request} and returns it with metadata. */
    public String review(ExpertReviewRequest request) {
    	
    	   log.info("Generating expert review for {} {}", request.brand(), request.model());

           String userPrompt = """
                   Generate a detailed expert review for this car:

                   Brand: %s
                   Model: %s
                   Variant: %s
                   Price: %s

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
                   - Return ONLY valid JSON matching the sample structure, with one entry in the
                     "reviews" array per section above (each with a "header" and a "text")
                   - Write in clear, professional English
                   - Be balanced and realistic
                   - Do not invent specific numbers you are not sure about
                   """.formatted(request.brand(), request.model(), request.variant(), request.price(), sampleJson());
           
           String jsonResponse = chatClient.prompt()
                   .system(SYSTEM)
                   .user(userPrompt)
                   .call()
                   .content();
           
           try {
               // Clean possible markdown if model still adds it
               jsonResponse = cleanJson(jsonResponse);

               JsonNode sections = objectMapper.readTree(jsonResponse).path("reviews");
               if (!sections.isArray() || sections.isEmpty()) {
                   throw new IllegalStateException("Review JSON has no 'reviews' sections");
               }

               log.info("Successfully generated expert review for {} {} | {} section(s)",
            		   request.brand(), request.model(), sections.size());

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

}
