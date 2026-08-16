package com.selectcar.agent.controller;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selectcar.agent.agent.ExpertReviewAgent;
import com.selectcar.agent.config.PipelineProperties;
import com.selectcar.agent.model.ExpertReview;
import com.selectcar.agent.model.ExpertReviewRequest;
import com.selectcar.agent.model.Model;
import com.selectcar.agent.model.ReviewStatus;
import com.selectcar.agent.service.ExpertReviewService;
import com.selectcar.agent.util.JsonStore;

@RestController
@RequestMapping("/api")
public class ExpertReviewController {

	private static final Logger log = LoggerFactory.getLogger(ExpertReviewController.class);
	
	@Autowired
	private	
	ExpertReviewAgent expertReviewAgent;
	
	@Autowired
	ExpertReviewService expertReviewService;
	
	@Autowired
	PipelineProperties properties;
	
	@Autowired
	JsonStore jsonStore;

    @GetMapping("/review")
    public String expertReview() {
        // TODO: Implement expert review logic
    	
    	try {
			// List<Model> models = readModelsFromJson("C:\\xampp\\selectcar\\data\\model-index.json");
			List<Model> models = readModelsFromResource("model-index.json");
			models.forEach(model -> {
				String review = expertReviewService.generateReviewSummary(model);
				//ExpertReviewRequest request = ExpertReviewRequest.of(model.getBrand(), model.getModel());
				//ExpertReview review = expertReviewAgent.review(request);
				System.out.println("Expert review for " + model.getBrand() + " " + model.getModel() + ": " + review);
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return "Expert review";
    }
    
    @GetMapping("/lookup/{brand}/{model}")
    public ExpertReview lookup(@PathVariable(name = "brand") String brand, @PathVariable(name = "model") String model) {
        List<Model> models;
        try {
            models = readModelsFromResource("model-index.json");
        } catch (Exception e) {
            log.error("Failed to read model index", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Model index unavailable");
        }

        Model match = models.stream()
                .filter(m -> m.getBrand() != null && m.getBrand().equalsIgnoreCase(brand)
                        && m.getModel() != null && m.getModel().equalsIgnoreCase(model))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Unknown model: " + brand + " " + model));

        ExpertReviewRequest request = ExpertReviewRequest.of(
                match.getBrand(),
                match.getModel(),
                ExpertReviewService.getPriceRange(match),
                ExpertReviewService.getVariantNamesAndCount(match));
        return expertReviewAgent.review(request);
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
    
    
    public boolean isReviewAlreadyDone(Model model) throws Exception {
        if (model == null || model.getBrand() == null || model.getModel() == null) {
            return false;
        }

        // Read path from properties (resolved against the configured data directory)
        String filePath = properties.getReviewStatus();

        // If tracking file does not exist yet → nothing is done
        if (!jsonStore.exists(filePath)) {
            return false;
        }

        List<ReviewStatus> statusList = jsonStore.read(filePath, new TypeReference<List<ReviewStatus>>() {
        });

        String brand = model.getBrand().trim();
        String modelName = model.getModel().trim();

        for (ReviewStatus status : statusList) {
            if (status.getBrand() != null
                    && status.getModel() != null
                    && status.getBrand().equalsIgnoreCase(brand)
                    && status.getModel().equalsIgnoreCase(modelName)
                    && "done".equalsIgnoreCase(status.getStatus())) {
                return true;
            }
        }

        return false;
    }
    
    
}
