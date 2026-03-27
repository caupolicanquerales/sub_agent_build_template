package com.capo.sub_agent_build_template.services;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.capo.sub_agent_build_template.utils.AsyncUtil;
import com.capo.sub_agent_build_template.utils.ToolContextHolder;


@Service
public class ExecutingBuildTemplateService {
	
	private final ChatClient chatClient;
	private final String systemPrompt;
	
	public ExecutingBuildTemplateService(@Qualifier("chatClientBuildTemplate") ChatClient chatClient,
			@Qualifier("systemPrompt") String systemPrompt) {
		this.chatClient = chatClient;
		this.systemPrompt= systemPrompt;
	}
	
	public CompletableFuture<String> generateBuildTemplateAsync(String prompt, String sessionId){

		Supplier<String> supplier = () -> {
			ToolContextHolder.set(sessionId);
			try {
				return this.chatClient.prompt()
						.messages(new SystemMessage(systemPrompt))
						.user(prompt)
						.advisors(a -> a.param("chat_memory_conversation_id", sessionId))
						.call()
						.content();
			} finally {
				ToolContextHolder.clear();
			}
		};

		return AsyncUtil.executeAsync(supplier, "Async error generating build template");
	}
	
}
