package com.selectcar.agent;

import com.selectcar.agent.util.PublishDateParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PublishDateParserTest {

    @Test
    void parsesIsoInstant() {
        assertThat(PublishDateParser.parse("2026-07-20T10:15:30Z"))
                .isEqualTo(Instant.parse("2026-07-20T10:15:30Z"));
    }

    @Test
    void parsesOffsetDateTime() {
        assertThat(PublishDateParser.parse("2026-07-20T10:15:30+02:00"))
                .isEqualTo(Instant.parse("2026-07-20T08:15:30Z"));
    }

    @Test
    void parsesPlainDate() {
        assertThat(PublishDateParser.parse("2026-07-20"))
                .isEqualTo(Instant.parse("2026-07-20T00:00:00Z"));
    }

    @Test
    void returnsNullForGarbage() {
        assertThat(PublishDateParser.parse("not-a-date")).isNull();
        assertThat(PublishDateParser.parse(null)).isNull();
    }

    @Test
    void readsPublishedTimeMetaFromDocument() {
        Document doc = Jsoup.parse("""
                <html><head>
                <meta property="article:published_time" content="2026-07-25T09:00:00Z"/>
                </head><body></body></html>
                """);
        assertThat(PublishDateParser.fromDocument(doc))
                .isEqualTo(Instant.parse("2026-07-25T09:00:00Z"));
    }
}
