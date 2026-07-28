package com.selectcar.agent.util;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Best-effort extraction of an article's publication timestamp from common HTML/meta
 * conventions (Open Graph, {@code article:published_time}, {@code <time datetime>} and
 * JSON-LD {@code datePublished}). Returns {@code null} when nothing usable is found.
 */
public final class PublishDateParser {

    private static final List<String> META_SELECTORS = List.of(
            "meta[property=article:published_time]",
            "meta[name=article:published_time]",
            "meta[property=og:published_time]",
            "meta[name=publish-date]",
            "meta[name=publishdate]",
            "meta[name=date]",
            "meta[itemprop=datePublished]",
            "meta[name=DC.date.issued]");

    private PublishDateParser() {
    }

    public static Instant fromDocument(Document doc) {
        for (String selector : META_SELECTORS) {
            Element el = doc.selectFirst(selector);
            if (el != null) {
                Instant parsed = parse(el.attr("content"));
                if (parsed != null) {
                    return parsed;
                }
            }
        }

        Element time = doc.selectFirst("time[datetime]");
        if (time != null) {
            Instant parsed = parse(time.attr("datetime"));
            if (parsed != null) {
                return parsed;
            }
        }

        for (Element ld : doc.select("script[type=application/ld+json]")) {
            Instant parsed = parseFromJsonLd(ld.data());
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private static Instant parseFromJsonLd(String json) {
        if (json == null) {
            return null;
        }
        int idx = json.indexOf("datePublished");
        if (idx < 0) {
            return null;
        }
        int colon = json.indexOf(':', idx);
        if (colon < 0) {
            return null;
        }
        int firstQuote = json.indexOf('"', colon);
        if (firstQuote < 0) {
            return null;
        }
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return null;
        }
        return parse(json.substring(firstQuote + 1, secondQuote));
    }

    public static Instant parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            // try next
        }
        try {
            return java.time.OffsetDateTime.parse(value).toInstant();
        } catch (Exception ignored) {
            // try next
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
                    .atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (Exception ignored) {
            // give up
        }
        return null;
    }
}
