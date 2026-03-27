package com.capo.sub_agent_build_template.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class EmitterRegistry {
	
	private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void register(String id, SseEmitter emitter) {
        emitters.put(id, emitter);
        emitter.onCompletion(() -> emitters.remove(id));
        emitter.onTimeout(() -> emitters.remove(id));
    }

    public SseEmitter get(String id) {
        return emitters.get(id);
    }
    
    public void remove(String id) {
        if (id != null) {
            emitters.remove(id);
        }
    }
}
