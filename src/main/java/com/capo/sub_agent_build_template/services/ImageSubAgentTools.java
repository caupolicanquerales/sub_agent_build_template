package com.capo.sub_agent_build_template.services;


import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.capo.sub_agent_build_template.request.SubAgentRequest;

import org.springframework.http.MediaType;

@Component
public class ImageSubAgentTools {
	
	private final WebClient webClient;
	private final RedisTemplate<String, Object> redisTemplate;
	
	@Value(value="${url-image}")
	private String urlImage;
	
	public ImageSubAgentTools(WebClient.Builder builder, RedisTemplate<String, Object> redisTemplate) {
        this.webClient = builder
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(20 * 1024 * 1024)) // 20 MB — needed for large base64 images
                .build();
        this.redisTemplate = redisTemplate;
    }
	
	@Tool(description = "Calls the image sub-agent via SSE to generate an image based on a prompt. Returns an image reference key (NOT the raw Base64). Pass this key as-is to saveHtmlDataTool.")
    public String generateImageWithSse(String prompt) {
        String imageData = webClient.post()
        		.uri(urlImage)
                .bodyValue(setSubAgentRequest(prompt))
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .filter(sse -> sse.data() != null && !sse.data().isBlank())
                .timeout(Duration.ofSeconds(300))
                .last()
                .map(ServerSentEvent::data)
                .onErrorReturn("Image generation failed")
                .block(Duration.ofSeconds(300));

        if (imageData == null || "Image generation failed".equals(imageData)) {
            return "Image generation failed";
        }

        String imageKey = "img_" + UUID.randomUUID();
        redisTemplate.opsForValue().set(imageKey, imageData, 10, TimeUnit.MINUTES);
        return "__IMG_KEY__:" + imageKey;
    }
	
	private SubAgentRequest setSubAgentRequest(String prompt) {
		SubAgentRequest request= new SubAgentRequest();
		request.setPrompt(prompt);
		return request;
	}
}
