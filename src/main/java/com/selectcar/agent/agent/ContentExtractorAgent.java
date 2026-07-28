package com.selectcar.agent.agent;

import com.selectcar.agent.config.PipelineProperties;
import com.selectcar.agent.model.ArticleListItem;
import com.selectcar.agent.model.ScannedLink;
import com.selectcar.agent.util.HtmlFetcher;
import com.selectcar.agent.util.PublishDateParser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent 5 — fetches each candidate page, extracts a title + summary, resolves the
 * publication date and enforces the recency window. Produces the {@code articlelist.json}
 * entries.
 */
@Component
public class ContentExtractorAgent {

    private static final Logger log = LoggerFactory.getLogger(ContentExtractorAgent.class);
    private static final int SUMMARY_MAX_CHARS = 600;

    private final HtmlFetcher htmlFetcher;
    private final PipelineProperties properties;

    public ContentExtractorAgent(HtmlFetcher htmlFetcher, PipelineProperties properties) {
        this.htmlFetcher = htmlFetcher;
        this.properties = properties;
    }

    public List<ArticleListItem> extract(List<ScannedLink> links) {
        Instant cutoff = Instant.now().minus(properties.getLookbackDays(), ChronoUnit.DAYS);
        List<ArticleListItem> items = new ArrayList<>();
        for (ScannedLink link : links) {
            try {
                ArticleListItem item = extractOne(link, cutoff);
                if (item != null) {
                    items.add(item);
                }
            } catch (Exception e) {
                log.warn("Failed to extract {}: {}", link.url(), e.toString());
            }
        }
        log.info("Extraction complete: {} recent article(s) ready for generation", items.size());
        return items;
    }

    private ArticleListItem extractOne(ScannedLink link, Instant cutoff) throws Exception {
        Document doc = htmlFetcher.fetch(link.url());

        Instant publishedAt = link.publishedAt();
        if (publishedAt == null) {
            publishedAt = PublishDateParser.fromDocument(doc);
        }
        if (publishedAt != null && publishedAt.isBefore(cutoff)) {
            log.debug("Skipping {} (published {} before cutoff {})", link.url(), publishedAt, cutoff);
            return null;
        }

        String title = firstNonBlank(
                metaContent(doc, "meta[property=og:title]"),
                metaContent(doc, "meta[name=twitter:title]"),
                text(doc.selectFirst("h1")),
                doc.title(),
                link.anchorText());

        String summary = firstNonBlank(
                metaContent(doc, "meta[property=og:description]"),
                metaContent(doc, "meta[name=description]"),
                firstParagraph(doc));

        if (title == null || title.isBlank()) {
            return null;
        }
        return new ArticleListItem(link.url(), link.sourceName(), title.trim(),
                truncate(summary), publishedAt);
    }

    private String firstParagraph(Document doc) {
        for (Element p : doc.select("article p, main p, p")) {
            String text = p.text().trim();
            if (text.length() >= 80) {
                return text;
            }
        }
        return null;
    }

    private String metaContent(Document doc, String selector) {
        Element el = doc.selectFirst(selector);
        return el == null ? null : el.attr("content");
    }

    private String text(Element el) {
        return el == null ? null : el.text();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= SUMMARY_MAX_CHARS ? trimmed : trimmed.substring(0, SUMMARY_MAX_CHARS) + "...";
    }
}
