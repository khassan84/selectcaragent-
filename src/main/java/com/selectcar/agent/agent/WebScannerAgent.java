package com.selectcar.agent.agent;

import com.selectcar.agent.config.PipelineProperties;
import com.selectcar.agent.model.ScannedLink;
import com.selectcar.agent.model.Source;
import com.selectcar.agent.util.HtmlFetcher;
import com.selectcar.agent.util.PublishDateParser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 2 — scans each source homepage and collects candidate article links. When a link
 * has a nearby {@code <time>} element the publication date is captured as an early hint;
 * the authoritative recency check happens later once each article page is fetched.
 */
@Component
public class WebScannerAgent {

    private static final Logger log = LoggerFactory.getLogger(WebScannerAgent.class);

    private final HtmlFetcher htmlFetcher;
    private final PipelineProperties properties;

    public WebScannerAgent(HtmlFetcher htmlFetcher, PipelineProperties properties) {
        this.htmlFetcher = htmlFetcher;
        this.properties = properties;
    }

    public List<ScannedLink> scan(List<Source> sources) {
        List<ScannedLink> all = new ArrayList<>();
        for (Source source : sources) {
            try {
                all.addAll(scanSource(source));
            } catch (Exception e) {
                log.warn("Failed to scan source {} ({}): {}", source.name(), source.url(), e.toString());
            }
        }
        log.info("Scan complete: {} candidate link(s) across {} source(s)", all.size(), sources.size());
        return all;
    }

    private List<ScannedLink> scanSource(Source source) throws Exception {
        Document doc = htmlFetcher.fetch(source.url());
        String baseHost = hostOf(source.url());
        Map<String, ScannedLink> unique = new LinkedHashMap<>();

        for (Element anchor : doc.select("a[href]")) {
            if (unique.size() >= properties.getMaxLinksPerSource()) {
                break;
            }
            String url = anchor.absUrl("href");
            if (!isCandidate(url, source.url(), baseHost)) {
                continue;
            }
            String anchorText = anchor.text().trim();
            Instant hint = nearbyPublishHint(anchor);
            unique.putIfAbsent(url, new ScannedLink(url, source.name(), anchorText, hint));
        }

        List<ScannedLink> links = new ArrayList<>(unique.values());
        log.info("Scanned {} -> {} candidate link(s)", source.name(), links.size());
        return links;
    }

    private boolean isCandidate(String url, String homepage, String baseHost) {
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
            return false;
        }
        if (url.equals(homepage) || url.equals(homepage + "/")) {
            return false;
        }
        String host = hostOf(url);
        if (baseHost != null && host != null && !host.endsWith(baseHost) && !baseHost.endsWith(host)) {
            return false;
        }
        String lower = url.toLowerCase();
        // Skip obvious non-article destinations.
        return !(lower.contains("/tag/") || lower.contains("/category/")
                || lower.contains("/author/") || lower.contains("/login")
                || lower.contains("/privacy") || lower.contains("/terms")
                || lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".pdf"));
    }

    private Instant nearbyPublishHint(Element anchor) {
        Element parent = anchor.parent();
        for (int depth = 0; depth < 3 && parent != null; depth++) {
            Element time = parent.selectFirst("time[datetime]");
            if (time != null) {
                Instant parsed = PublishDateParser.parse(time.attr("datetime"));
                if (parsed != null) {
                    return parsed;
                }
            }
            parent = parent.parent();
        }
        return null;
    }

    private String hostOf(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? null : host.replaceFirst("^www\\.", "");
        } catch (Exception e) {
            return null;
        }
    }
}
