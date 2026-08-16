package com.selectcar.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the car-news pipeline. All values can be overridden through
 * {@code application.yml} or environment variables under the {@code selectcar} prefix.
 */
@ConfigurationProperties(prefix = "selectcar")
public class PipelineProperties {

    /** Directory (relative to the working dir or absolute) holding the local JSON files. */
    private String dataDir = "data";

    /** Name of the JSON file listing brand and news homepages. */
    private String sourcesFile = "website.json";

    /** File that stores URLs that have already been scanned/processed. */
    private String processedFile = "processed.json";

    /** Intermediate file produced by the extraction stage. */
    private String articleListFile = "articlelist.json";

    /** Final file holding the generated articles. */
    private String articlesFile = "articles.json";

    /** File holding the generated expert reviews. */
    private String expertReviewsFile = "expertreviews.json";

    /** Sample review JSON handed to the LLM as the structure to follow. */
    private String expertReviewSampleFile = "expertreviewsample.json";

    /** Directory (inside the data dir) where one file per generated review is stored. */
    private String expertReviewsDir = "expertreviews";

    /** How many days back an article may be published and still be considered "recent". */
    private int lookbackDays = 7;

    /** Maximum candidate links to follow per source homepage. */
    private int maxLinksPerSource = 25;

    /** Minimum word count required for a generated news article. */
    private int minArticleWords = 1200;

    /** Minimum word count required for a generated expert review. */
    private int minReviewWords = 1000;

    /** HTTP timeout in milliseconds for scraping requests. */
    private int httpTimeoutMs = 15000;
    
    private String reviewStatus = "reviewStatus.json";

    /** User-Agent used for scraping. */
    private String userAgent =
            "Mozilla/5.0 (compatible; SelectCarAgent/0.1; +https://github.com/khassan84/selectcaragent)";
    
    private boolean runPipeline = false;

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public String getSourcesFile() {
        return sourcesFile;
    }

    public void setSourcesFile(String sourcesFile) {
        this.sourcesFile = sourcesFile;
    }

    public String getProcessedFile() {
        return processedFile;
    }

    public void setProcessedFile(String processedFile) {
        this.processedFile = processedFile;
    }

    public String getArticleListFile() {
        return articleListFile;
    }

    public void setArticleListFile(String articleListFile) {
        this.articleListFile = articleListFile;
    }

    public String getArticlesFile() {
        return articlesFile;
    }

    public void setArticlesFile(String articlesFile) {
        this.articlesFile = articlesFile;
    }

    public String getExpertReviewsFile() {
        return expertReviewsFile;
    }

    public void setExpertReviewsFile(String expertReviewsFile) {
        this.expertReviewsFile = expertReviewsFile;
    }

    public String getExpertReviewSampleFile() {
        return expertReviewSampleFile;
    }

    public void setExpertReviewSampleFile(String expertReviewSampleFile) {
        this.expertReviewSampleFile = expertReviewSampleFile;
    }

    public String getExpertReviewsDir() {
        return expertReviewsDir;
    }

    public void setExpertReviewsDir(String expertReviewsDir) {
        this.expertReviewsDir = expertReviewsDir;
    }

    public int getLookbackDays() {
        return lookbackDays;
    }

    public void setLookbackDays(int lookbackDays) {
        this.lookbackDays = lookbackDays;
    }

    public int getMaxLinksPerSource() {
        return maxLinksPerSource;
    }

    public void setMaxLinksPerSource(int maxLinksPerSource) {
        this.maxLinksPerSource = maxLinksPerSource;
    }

    public int getMinArticleWords() {
        return minArticleWords;
    }

    public void setMinArticleWords(int minArticleWords) {
        this.minArticleWords = minArticleWords;
    }

    public int getMinReviewWords() {
        return minReviewWords;
    }

    public void setMinReviewWords(int minReviewWords) {
        this.minReviewWords = minReviewWords;
    }

    public int getHttpTimeoutMs() {
        return httpTimeoutMs;
    }

    public void setHttpTimeoutMs(int httpTimeoutMs) {
        this.httpTimeoutMs = httpTimeoutMs;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public boolean isRunPipeline() {
		return runPipeline;
	}
    
    public void setRunPipeline(boolean runPipeline) {
    			this.runPipeline = runPipeline;
    }
    
    public String getReviewStatus() {
		return reviewStatus;
	}
    
    public void setReviewStatus(String reviewStatus) {
		this.reviewStatus = reviewStatus;
	}
    
}
