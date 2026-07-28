package com.selectcar.agent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Agent 7 — condenses a full article into a short, punchy social-media story/caption.
 */
@Component
public class SocialStoryAgent {

    private static final String SYSTEM = """
            You are a social-media editor for a car brand. Write short, energetic posts that
            hook readers in the first line. Return only the post text (you may include a few
            relevant hashtags on the final line).
            """;

    private final ChatClient chatClient;

    public SocialStoryAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String createShortStory(String title, String article) {
        String prompt = """
                Turn the following car news article into a short social-media story of 60-120
                words suitable for Instagram or Facebook. Keep it lively and shareable.

                Title: %s

                Article:
                %s
                """.formatted(title, article);
        return chatClient.prompt().system(SYSTEM).user(prompt).call().content();
    }
}
