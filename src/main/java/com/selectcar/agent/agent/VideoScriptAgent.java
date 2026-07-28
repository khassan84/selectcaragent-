package com.selectcar.agent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Agent 8 — produces a short-form (Reels/Shorts/TikTok) video generation script from an
 * article, including scene directions, on-screen text and a voice-over.
 */
@Component
public class VideoScriptAgent {

    private static final String SYSTEM = """
            You are a scriptwriter for short vertical car videos (30-45 seconds). Produce a
            shot-by-shot script the video team and a text-to-video tool can follow.
            For each scene provide: SCENE number, VISUAL description, ON-SCREEN TEXT and
            VOICEOVER. End with a short call to action.
            """;

    private final ChatClient chatClient;

    public VideoScriptAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String createVideoScript(String title, String article) {
        String prompt = """
                Write a 30-45 second short-video script based on the following car news article.
                Use 4-6 scenes.

                Title: %s

                Article:
                %s
                """.formatted(title, article);
        return chatClient.prompt().system(SYSTEM).user(prompt).call().content();
    }
}
