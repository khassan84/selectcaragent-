package com.selectcar.agent.util;

import com.selectcar.agent.config.PipelineProperties;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Thin wrapper around Jsoup that applies the configured timeout and user-agent. */
@Component
public class HtmlFetcher {

    private final PipelineProperties properties;

    public HtmlFetcher(PipelineProperties properties) {
        this.properties = properties;
    }

    public Document fetch(String url) throws IOException {
        Connection connection = Jsoup.connect(url)
                .userAgent(properties.getUserAgent())
                .timeout(properties.getHttpTimeoutMs())
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .maxBodySize(0);
        return connection.get();
    }
}
