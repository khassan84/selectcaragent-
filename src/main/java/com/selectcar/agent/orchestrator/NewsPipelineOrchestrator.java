package com.selectcar.agent.orchestrator;

import com.selectcar.agent.agent.ArticleWriterAgent;
import com.selectcar.agent.agent.CarRelevanceFilterAgent;
import com.selectcar.agent.agent.ContentExtractorAgent;
import com.selectcar.agent.agent.DeduplicationAgent;
import com.selectcar.agent.agent.ExpertReviewAgent;
import com.selectcar.agent.agent.SocialStoryAgent;
import com.selectcar.agent.agent.SourceReaderAgent;
import com.selectcar.agent.agent.VideoScriptAgent;
import com.selectcar.agent.agent.WebScannerAgent;
import com.selectcar.agent.config.PipelineProperties;
import com.selectcar.agent.model.ArticleListItem;
import com.selectcar.agent.model.ExpertReview;
import com.selectcar.agent.model.ExpertReviewRequest;
import com.selectcar.agent.model.GeneratedArticle;
import com.selectcar.agent.model.ScannedLink;
import com.selectcar.agent.model.Source;
import com.selectcar.agent.util.JsonStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates the multi-agent car-news pipeline end to end:
 *
 * <ol>
 *   <li>read sources ({@code website.json})</li>
 *   <li>scan homepages for candidate links</li>
 *   <li>filter out non-car links</li>
 *   <li>drop already-processed links ({@code processed.json})</li>
 *   <li>extract title/summary + enforce recency, write {@code articlelist.json}</li>
 *   <li>generate article, social story and video script via Ollama, write {@code articles.json}</li>
 * </ol>
 *
 * <p>{@link #generateExpertReview(ExpertReviewRequest)} is an independent, on-demand stage: it
 * reviews one brand/model instead of consuming the scanned news links.</p>
 */
@Component
public class NewsPipelineOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(NewsPipelineOrchestrator.class);

    private final SourceReaderAgent sourceReaderAgent;
    private final WebScannerAgent webScannerAgent;
    private final CarRelevanceFilterAgent carRelevanceFilterAgent;
    private final DeduplicationAgent deduplicationAgent;
    private final ContentExtractorAgent contentExtractorAgent;
    private final ArticleWriterAgent articleWriterAgent;
    private final SocialStoryAgent socialStoryAgent;
    private final VideoScriptAgent videoScriptAgent;
    private final ExpertReviewAgent expertReviewAgent;
    private final JsonStore jsonStore;
    private final PipelineProperties properties;

    public NewsPipelineOrchestrator(SourceReaderAgent sourceReaderAgent,
                                    WebScannerAgent webScannerAgent,
                                    CarRelevanceFilterAgent carRelevanceFilterAgent,
                                    DeduplicationAgent deduplicationAgent,
                                    ContentExtractorAgent contentExtractorAgent,
                                    ArticleWriterAgent articleWriterAgent,
                                    SocialStoryAgent socialStoryAgent,
                                    VideoScriptAgent videoScriptAgent,
                                    ExpertReviewAgent expertReviewAgent,
                                    JsonStore jsonStore,
                                    PipelineProperties properties) {
        this.sourceReaderAgent = sourceReaderAgent;
        this.webScannerAgent = webScannerAgent;
        this.carRelevanceFilterAgent = carRelevanceFilterAgent;
        this.deduplicationAgent = deduplicationAgent;
        this.contentExtractorAgent = contentExtractorAgent;
        this.articleWriterAgent = articleWriterAgent;
        this.socialStoryAgent = socialStoryAgent;
        this.videoScriptAgent = videoScriptAgent;
        this.expertReviewAgent = expertReviewAgent;
        this.jsonStore = jsonStore;
        this.properties = properties;
    }

    /** Stages 1-5: build and persist {@code articlelist.json}. */
    public List<ArticleListItem> buildArticleList() {
        List<Source> sources = sourceReaderAgent.readSources();
        List<ScannedLink> scanned = webScannerAgent.scan(sources);
        List<ScannedLink> carLinks = carRelevanceFilterAgent.filter(scanned);
        List<ScannedLink> freshLinks = deduplicationAgent.removeProcessed(carLinks);
        List<ArticleListItem> articleList = contentExtractorAgent.extract(freshLinks);
        jsonStore.write(properties.getArticleListFile(), articleList);
        log.info("Wrote {} entries to {}", articleList.size(), properties.getArticleListFile());
        return articleList;
    }

    /** Stage 6: generate content for every item in {@code articlelist.json} via Ollama. */
    public List<GeneratedArticle> generateArticles() {
        List<ArticleListItem> articleList = readArticleList();
        List<GeneratedArticle> generated = new ArrayList<>();
        List<String> processedUrls = new ArrayList<>();

        for (ArticleListItem item : articleList) {
            try {
                String article = articleWriterAgent.writeArticle(item);
                String socialStory = socialStoryAgent.createShortStory(item.title(), article);
                String videoScript = videoScriptAgent.createVideoScript(item.title(), article);

                generated.add(new GeneratedArticle(
                        item.url(),
                        item.sourceName(),
                        item.title(),
                        article,
                        articleWriterAgent.wordCount(article),
                        socialStory,
                        videoScript,
                        Instant.now()));
                processedUrls.add(item.url());
            } catch (Exception e) {
                log.error("Generation failed for {}: {}", item.url(), e.toString());
            }
        }

        jsonStore.write(properties.getArticlesFile(), generated);
        if (!processedUrls.isEmpty()) {
            deduplicationAgent.markProcessed(processedUrls);
        }
        log.info("Wrote {} generated article(s) to {}", generated.size(), properties.getArticlesFile());
        return generated;
    }

    /**
     * On-demand stage: generate an expert review for a single brand/model and append it to
     * {@code expertreviews.json}.
     */
    public ExpertReview generateExpertReview(ExpertReviewRequest request) {
        ExpertReview review = expertReviewAgent.review(request);
        String file = properties.getExpertReviewsFile();
        List<ExpertReview> reviews = new ArrayList<>(readExpertReviews());
        reviews.add(review);
        jsonStore.write(file, reviews);
        log.info("Wrote expert review for '{}' to {}", request.displayName(), file);
        return review;
    }

    private List<ExpertReview> readExpertReviews() {
        String file = properties.getExpertReviewsFile();
        if (!jsonStore.exists(file)) {
            return List.of();
        }
        return jsonStore.read(file, new com.fasterxml.jackson.core.type.TypeReference<List<ExpertReview>>() {
        });
    }

    private List<ArticleListItem> readArticleList() {
        String file = properties.getArticleListFile();
        if (!jsonStore.exists(file)) {
            return List.of();
        }
        return jsonStore.read(file, new com.fasterxml.jackson.core.type.TypeReference<List<ArticleListItem>>() {
        });
    }

    /** Runs the full pipeline (stages 1-6). */
    public List<GeneratedArticle> run() {
        buildArticleList();
        return generateArticles();
    }
}
