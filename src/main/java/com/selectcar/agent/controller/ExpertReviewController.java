package com.selectcar.agent.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.selectcar.agent.agent.ExpertReviewAgent;

@RestController
@RequestMapping("/api/expert-review")
public class ExpertReviewController {
	
	@Autowired
	private	
	ExpertReviewAgent expertReviewAgent;

    @GetMapping
    public String expertReview() {
        // TODO: Implement expert review logic
        return "Expert review";
    }
}
