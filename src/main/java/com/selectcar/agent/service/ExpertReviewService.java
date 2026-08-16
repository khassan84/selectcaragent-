package com.selectcar.agent.service;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selectcar.agent.agent.ExpertReviewAgent2;
import com.selectcar.agent.model.ExpertReviewRequest;
import com.selectcar.agent.model.Model;
import com.selectcar.agent.model.Variant;

@Component
public class ExpertReviewService {
	
	
	@Autowired
	ExpertReviewAgent2 expertReviewAgent2;
	
	
	public String generateReviewSummary(Model model) {
	    if (model == null) {
	        return "No model data available.";
	    }
	    String price = getPriceRange(model);
	    String variantInfo = getVariantNamesAndCount(model);
	    ExpertReviewRequest request = ExpertReviewRequest.of(model.getBrand(), model.getModel(), price, variantInfo);
	    String response = expertReviewAgent2.review(request);

	    return response;
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
