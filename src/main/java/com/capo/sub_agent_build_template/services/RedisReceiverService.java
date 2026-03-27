package com.capo.sub_agent_build_template.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.capo.redis_object.ImageRedisResponse;
import com.capo.sub_agent_build_template.utils.EmitterRegistry;

@Service
public class RedisReceiverService {
	
	private final RedisTemplate<String, Object> redisTemplate;
	private final EmitterRegistry registry;
	
	@Value(value="${event.name.image}")
	private String eventName;
	
	
	public RedisReceiverService(RedisTemplate<String, Object> redisTemplate,
			EmitterRegistry registry) {
		this.redisTemplate= redisTemplate;
		this.registry= registry;
	}
	
	public void receiveMessage(Map<String,String> message) {
		String requestId = message.get("id");
		String sessionId = message.get("sessionId");
		String status = message.get("status");
		String emitterKey = (sessionId != null) ? sessionId : requestId;
		SseEmitter emitter = registry.get(emitterKey);
		if(emitter != null) {
			try {
				if ("ERROR".equals(status)) {
					emitter.send(SseEmitter.event().name(eventName).data("ERROR: image generation failed"));
					emitter.complete();
					return;
				}
				ImageRedisResponse information = (ImageRedisResponse) redisTemplate.opsForValue().get(requestId);
				if (information == null) {
					emitter.send(SseEmitter.event().name(eventName).data("ERROR: result not found"));
					emitter.complete();
					return;
				}
				emitter.send(SseEmitter.event().name(eventName).data(information.getImage()));
				emitter.complete();
			} catch (Exception e) {
				emitter.completeWithError(e);
			} finally {
				registry.remove(emitterKey);
			}
		}
	}
	
}
