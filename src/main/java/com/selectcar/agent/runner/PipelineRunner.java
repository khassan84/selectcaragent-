package com.selectcar.agent.runner;

import com.selectcar.agent.orchestrator.NewsPipelineOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Entry point that runs the pipeline when the application starts.
 *
 * <p>Command-line stage selection:</p>
 * <ul>
 *   <li>{@code --stage=all} (default) — run scan + generation</li>
 *   <li>{@code --stage=scan} — stages 1-5 only, producing {@code articlelist.json}</li>
 *   <li>{@code --stage=generate} — stage 6 only, from an existing {@code articlelist.json}</li>
 * </ul>
 *
 * <p>Disable auto-run with {@code --selectcar.run-on-startup=false}.</p>
 */
@Component
public class PipelineRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PipelineRunner.class);

    private final NewsPipelineOrchestrator orchestrator;

    public PipelineRunner(NewsPipelineOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (isDisabled(args)) {
            log.info("Pipeline auto-run disabled (--selectcar.run-on-startup=false). Nothing to do.");
            return;
        }
        String stage = stage(args);
        log.info("Starting selectcaragent pipeline (stage='{}')", stage);
        try {
            switch (stage) {
                case "scan" -> orchestrator.buildArticleList();
                case "generate" -> orchestrator.generateArticles();
                default -> orchestrator.run();
            }
            log.info("Pipeline finished.");
        } catch (Exception e) {
            log.error("Pipeline failed: {}", e.getMessage(), e);
        }
    }

    private boolean isDisabled(ApplicationArguments args) {
        List<String> values = args.getOptionValues("selectcar.run-on-startup");
        return values != null && !values.isEmpty() && values.get(0).equalsIgnoreCase("false");
    }

    private String stage(ApplicationArguments args) {
        List<String> values = args.getOptionValues("stage");
        if (values == null || values.isEmpty()) {
            return "all";
        }
        return values.get(0).toLowerCase();
    }
}
