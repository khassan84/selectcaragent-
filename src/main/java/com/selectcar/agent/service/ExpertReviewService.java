package com.selectcar.agent.service;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selectcar.agent.agent.ExpertReviewAgent2;
import com.selectcar.agent.config.PipelineProperties;
import com.selectcar.agent.model.ExpertReviewRequest;
import com.selectcar.agent.model.Model;
import com.selectcar.agent.model.ReviewStatus;
import com.selectcar.agent.model.Variant;
import com.selectcar.agent.util.JsonStore;

@Component
public class ExpertReviewService {

	private static final Logger log = LoggerFactory.getLogger(ExpertReviewService.class);

	@Autowired
	ExpertReviewAgent2 expertReviewAgent2;

	@Autowired
	PipelineProperties properties;

	@Autowired
	JsonStore jsonStore;


	public String generateReviewSummary(Model model) {
	    if (model == null) {
	        return "No model data available.";
	    }
	    String price = getPriceRange(model);
	    String variantInfo = getVariantNamesAndCount(model);
	    ExpertReviewRequest request = ExpertReviewRequest.of(model.getBrand(), model.getModel(), price, variantInfo);
	    String response = expertReviewAgent2.review(request);

	    saveReview(model, response);
	    markReviewDone(model);

	    return response;
	}

	/** Stores the generated review JSON as one file per model, e.g. {@code expertreviews/maruti-brezza.json}. */
	String saveReview(Model model, String reviewJson) {
	    String file = properties.getExpertReviewsDir() + "/" + reviewFileName(model);
	    jsonStore.writeString(file, reviewJson);
	    log.info("Saved expert review for {} {} to {}", model.getBrand(), model.getModel(), jsonStore.resolve(file));
	    return file;
	}

	/** Upserts the brand/model entry in {@code reviewStatus.json} with status {@code done}. */
	void markReviewDone(Model model) {
	    String file = properties.getReviewStatus();
	    List<ReviewStatus> statusList = jsonStore.exists(file)
	            ? new ArrayList<>(jsonStore.read(file, new TypeReference<List<ReviewStatus>>() {
	            }))
	            : new ArrayList<>();

	    ReviewStatus existing = statusList.stream()
	            .filter(status -> matches(status, model))
	            .findFirst()
	            .orElse(null);

	    if (existing == null) {
	        existing = new ReviewStatus();
	        existing.setBrand(model.getBrand());
	        existing.setModel(model.getModel());
	        statusList.add(existing);
	    }
	    existing.setStatus("done");

	    jsonStore.write(file, statusList);
	    log.info("Marked review status done for {} {} in {}", model.getBrand(), model.getModel(), file);
	}

	private boolean matches(ReviewStatus status, Model model) {
	    return status.getBrand() != null && status.getModel() != null
	            && status.getBrand().equalsIgnoreCase(nullToEmpty(model.getBrand()).trim())
	            && status.getModel().equalsIgnoreCase(nullToEmpty(model.getModel()).trim());
	}

	private String reviewFileName(Model model) {
	    String brand = model.getBrandSlug() != null && !model.getBrandSlug().isBlank()
	            ? model.getBrandSlug()
	            : nullToEmpty(model.getBrand());
	    String modelName = model.getSlug() != null && !model.getSlug().isBlank()
	            ? model.getSlug()
	            : nullToEmpty(model.getModel());
	    return slugify(brand + "-" + modelName) + ".json";
	}

	private String slugify(String value) {
	    String slug = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
	    return slug.isEmpty() ? "review" : slug;
	}

	private String nullToEmpty(String value) {
	    return value == null ? "" : value;
	}
	
	
	
	
	public static String getPriceRange(Model model) {
	    if (model == null || model.getVariants() == null || model.getVariants().isEmpty()) {
	        return "Price : N/A";
	    }

	    long minPrice = Long.MAX_VALUE;
	    long maxPrice = Long.MIN_VALUE;
	    boolean foundValidPrice = false;

	    for (Variant variant : model.getVariants()) {
	        if (variant.getPrice() == null || variant.getPrice().isBlank()) {
	            continue;
	        }

	        // Remove currency symbols, commas, spaces etc. and keep only digits
	        String numeric = variant.getPrice().replaceAll("[^0-9]", "");

	        if (numeric.isEmpty()) {
	            continue;
	        }

	        try {
	            long price = Long.parseLong(numeric);
	            minPrice = Math.min(minPrice, price);
	            maxPrice = Math.max(maxPrice, price);
	            foundValidPrice = true;
	        } catch (NumberFormatException e) {
	            // skip invalid price
	        }
	    }

	    if (!foundValidPrice) {
	        return "Price : N/A";
	    }

	    if (minPrice == maxPrice) {
	        return "Price : " + minPrice;
	    }

	    return "Price : " + minPrice + " - " + maxPrice;
	}
	
	
	public static String getVariantNamesAndCount(Model model) {
	    if (model == null || model.getVariants() == null || model.getVariants().isEmpty()) {
	        return "No variants";
	    }

	    List<String> names = model.getVariants().stream()
	            .map(Variant::getVariantName)
	            .filter(name -> name != null && !name.isBlank())
	            .toList();

	    if (names.isEmpty()) {
	        return "No variants";
	    }

	    String commaSeparated = String.join(", ", names);
	    int count = names.size();

	    return commaSeparated + " (" + count + " variant" + (count > 1 ? "s" : "") + ")";
	}
	
	
	 public static List<Model> readModelsFromJson(String filePath) throws Exception {

		    ObjectMapper objectMapper = new ObjectMapper();

		    List<Model> models =  objectMapper.readValue(new File(filePath), new TypeReference<List<Model>>() {});

		    System.out.println(models);
		    return models;
		} 
	    
    public List<Model> readModelsFromResource(String resourcePath) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        ClassPathResource resource = new ClassPathResource(resourcePath);
        
        if (!resource.exists()) {
            throw new RuntimeException("Resource not found: " + resourcePath);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<Model>>() {});
        }
    }

}
