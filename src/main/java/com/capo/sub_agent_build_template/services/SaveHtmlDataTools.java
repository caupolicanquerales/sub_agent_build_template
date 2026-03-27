package com.capo.sub_agent_build_template.services;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import com.capo.redis_object.ImageRedisRequest;
import com.capo.sub_agent_build_template.utils.ToolContextHolder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SaveHtmlDataTools {
	
	private final RedisTemplate<String, Object> redisTemplate;
	private final ChannelTopic requestTopic;
	private final ObjectMapper objectMapper;

	private static final Logger log = LoggerFactory.getLogger(SaveHtmlDataTools.class);
	
	public SaveHtmlDataTools(RedisTemplate<String, Object> redisTemplate,
			@Qualifier("requestTopic") ChannelTopic requestTopic,
			ObjectMapper objectMapper) {
		this.redisTemplate= redisTemplate;
		this.requestTopic= requestTopic;
		this.objectMapper= objectMapper;
	}
	
	@Tool(description = "Saves the extracted HTML template, structured data, and generated images for final processing")
    public String saveHtmlDataTool(
    		@ToolParam(description = "The extracted raw HTML/CSS code") String html,
    		@ToolParam(description = "The extracted raw JSON data object as a string") String data,
    		@ToolParam(description = "JSON object mapping image keys to the reference keys returned by generateImageWithSse (e.g. {\"img1\": \"__IMG_KEY__:img_uuid\"}), or empty object if no images") String images) {
		String id = ToolContextHolder.get();
		if (id == null) {
			log.error("Error in saveHtmlDataTool getting Id");
			return "Error: session context not available";
		}
		ImageRedisRequest payload = new ImageRedisRequest();
		payload.setHtml(html);
		payload.setData(data);
		payload.setImages(resolveImageKeys(images));
        redisTemplate.opsForValue().set(id, payload, 5, TimeUnit.MINUTES);
        redisTemplate.convertAndSend(requestTopic.getTopic(), Map.of("id", id));
        return "Processing started for ID: " + id;
	}

	private String resolveImageKeys(String imagesJson) {
		final String KEY_PREFIX = "__IMG_KEY__:";
		if (imagesJson == null || imagesJson.isBlank() || !imagesJson.contains(KEY_PREFIX)) {
			return imagesJson;
		}
		try {
			Map<String, String> imageMap = objectMapper.readValue(imagesJson, new TypeReference<Map<String, String>>() {});
			for (Map.Entry<String, String> entry : imageMap.entrySet()) {
				String value = entry.getValue();
				if (value != null && value.startsWith(KEY_PREFIX)) {
					String redisKey = value.substring(KEY_PREFIX.length());
					Object stored = redisTemplate.opsForValue().get(redisKey);
					if (stored instanceof String actualImage) {
						entry.setValue(actualImage);
						redisTemplate.delete(redisKey);
					}
				}
			}
			return objectMapper.writeValueAsString(imageMap);
		} catch (Exception e) {
			log.error("Error resolving image keys from images JSON: {}", e.getMessage());
			return imagesJson;
		}
	}
	
}
