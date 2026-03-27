package com.capo.sub_agent_build_template.controller;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.capo.sub_agent_build_template.request.GenerationSyntheticDataRequest;
import com.capo.sub_agent_build_template.services.ExecutingBuildTemplateService;
import com.capo.sub_agent_build_template.utils.EmitterRegistry;


@RestController
@RequestMapping("sub-agent-build-template")
public class SubAgentController {

	private static final Logger log = LoggerFactory.getLogger(SubAgentController.class);

	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final ExecutingBuildTemplateService executingBuild;
	private final EmitterRegistry emitterRegistry;
	
	@Value(value="${event.name.image}")
	private String eventName;
	
	public SubAgentController(ExecutingBuildTemplateService executingBuild,
			EmitterRegistry emitterRegistry) {
		this.executingBuild= executingBuild;
		this.emitterRegistry= emitterRegistry;
	}
	
	@PostMapping(path = "/stream-image", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamImageGeneration(@RequestBody GenerationSyntheticDataRequest request) {
		final SseEmitter emitter = new SseEmitter(300_000L);
		String id = UUID.randomUUID().toString();
		emitterRegistry.register(id, emitter);
		executor.execute(() -> {
			try {
				emitter.send(SseEmitter.event().name(eventName).data("Image generation started for prompt"));
				executingBuild.generateBuildTemplateAsync(request.getPrompt(), id)
					.exceptionally(ex -> {
						log.error("Error generating build template for id {}: {}", id, ex.getMessage());
						emitter.completeWithError(ex);
						emitterRegistry.remove(id);
						return null;
					});
			} catch (Exception e) {
				log.error("Error starting build template stream for id {}: {}", id, e.getMessage());
				emitter.completeWithError(e);
				emitterRegistry.remove(id);
			}
		});

        return emitter;
	}
}
