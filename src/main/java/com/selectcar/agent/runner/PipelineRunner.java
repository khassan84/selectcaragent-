package com.selectcar.agent.runner;

import com.selectcar.agent.model.ExpertReviewRequest;
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
 *   <li>{@code --stage=review} — expert review for one car, e.g.
 *       {@code --stage=review --brand=Toyota --model=Corolla --year=2025 --variant="GR Sport"
 *       --market=UK --price="from £30,995" --spec="140 kW hybrid" --focus="ride comfort"
 *       --notes="facelift"} ({@code --spec} and {@code --focus} may be repeated)</li>
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
                case "review" -> orchestrator.generateExpertReview(reviewRequest(args));
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

    private ExpertReviewRequest reviewRequest(ApplicationArguments args) {
        String brand = option(args, "brand");
        String model = option(args, "model");
        if (brand == null || model == null) {
            throw new IllegalArgumentException(
                    "--stage=review requires --brand=<brand> and --model=<model>");
        }
        return new ExpertReviewRequest(
                brand,
                model,
                intOption(args, "year"),
                option(args, "variant"),
                option(args, "market"),
                option(args, "price"),
                options(args, "spec"),
                options(args, "focus"),
                option(args, "notes"));
    }

    private String option(ApplicationArguments args, String name) {
        List<String> values = args.getOptionValues(name);
        if (values == null || values.isEmpty() || values.get(0).isBlank()) {
            return null;
        }
        return values.get(0);
    }

    private List<String> options(ApplicationArguments args, String name) {
        List<String> values = args.getOptionValues(name);
        return values == null ? List.of() : values;
    }

    private Integer intOption(ApplicationArguments args, String name) {
        String value = option(args, name);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--" + name + " must be a number, got '" + value + "'");
        }
    }

    private String stage(ApplicationArguments args) {
        List<String> values = args.getOptionValues("stage");
        if (values == null || values.isEmpty()) {
            return "all";
        }
        return values.get(0).toLowerCase();
    }
}
