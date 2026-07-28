package com.selectcar.agent;

import com.selectcar.agent.config.PipelineProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PipelineProperties.class)
public class SelectCarAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SelectCarAgentApplication.class, args);
    }
}
