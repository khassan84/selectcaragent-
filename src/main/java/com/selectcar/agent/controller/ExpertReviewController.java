package com.selectcar.agent.controller;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        // Create a minimal request with brand and model and delegate to the agent
    	
    	try {
			List<Model> models = readModelsFromResource("model-index.json");
	        ExpertReviewRequest request = ExpertReviewRequest.of(brand, model);
	        models.forEach(m-> {
	        	if (m.getBrand().equalsIgnoreCase(brand) && m.getModel().equalsIgnoreCase(model)) {
	        		String review = expertReviewService.generateReviewSummary(m);
	        		System.out.println("Expert review for " + m.getBrand() + " " + m.getModel() + ": " + review);
	        	}
	        });
	        return expertReviewAgent.review(request);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return null;

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

        // Read path from properties (should point to data/review_status.json)
        String filePath = properties.getReviewStatus();
        File reviewStatusFile = new File(filePath);
        
        List<ReviewStatus> statusList = jsonStore.read(filePath, new TypeReference<List<ReviewStatus>>() {
        });

        // If tracking file does not exist yet → nothing is done
        if (!reviewStatusFile.exists()) {
            return false;
        }


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
