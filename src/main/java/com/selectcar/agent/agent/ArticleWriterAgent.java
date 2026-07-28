package com.selectcar.agent.agent;

import com.selectcar.agent.config.PipelineProperties;
import com.selectcar.agent.model.ArticleListItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Agent 6 — sends each extracted title/summary to the local Ollama model and asks for a
 * full news article of at least the configured minimum word count. If the first draft is
 * too short it asks the model once to expand it.
 */
@Component
public class ArticleWriterAgent {

    private static final Logger log = LoggerFactory.getLogger(ArticleWriterAgent.class);

    private static final String SYSTEM = """
            You are an experienced automotive journalist writing for a car-enthusiast website.
            Write factual, engaging, well-structured news articles in clear English.
            Use short paragraphs and do not invent specific numbers that were not provided.
            Return only the article body, with no preamble, markdown headers or word-count notes.
            """;

    private final ChatClient chatClient;
    private final PipelineProperties properties;

    public ArticleWriterAgent(ChatClient chatClient, PipelineProperties properties) {
        this.chatClient = chatClient;
        this.properties = properties;
    }

    public String writeArticle(ArticleListItem item) {
        int minWords = properties.getMinArticleWords();
        String prompt = """
                Write a detailed automotive news article of at least %d words based on the
                following source information.

                Title: %s
                Source: %s
                URL: %s
                Summary: %s

                The article must have an engaging introduction, a detailed body covering the key
                points and implications for car buyers/enthusiasts, and a concluding paragraph.
                """.formatted(minWords, item.title(), item.sourceName(), item.url(),
                item.summary() == null ? "" : item.summary());

        String article = chat(prompt);
        if (wordCount(article) < minWords) {
            log.info("Draft for '{}' was {} words; requesting expansion", item.title(), wordCount(article));
            article = chat("""
                    The following article is too short. Expand it to at least %d words by adding
                    more detail, context and analysis, while keeping all existing information.
                    Return only the expanded article body.

                    %s
                    """.formatted(minWords, article));
        }
        log.info("Generated article '{}' ({} words)", item.title(), wordCount(article));
        return article;
    }

    public int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private String chat(String userPrompt) {
        return chatClient.prompt().system(SYSTEM).user(userPrompt).call().content();
    }
}
