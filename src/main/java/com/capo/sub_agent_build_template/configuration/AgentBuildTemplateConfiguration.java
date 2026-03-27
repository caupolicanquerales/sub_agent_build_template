package com.capo.sub_agent_build_template.configuration;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.capo.sub_agent_build_template.services.ImageSubAgentTools;
import com.capo.sub_agent_build_template.services.SaveHtmlDataTools;


@Configuration
public class AgentBuildTemplateConfiguration {
	
	@Value("classpath:prompts/system-prompt.md")
    private Resource systemPromptResource;
	

	@Bean
    public String systemPrompt() throws IOException {
        return systemPromptResource.getContentAsString(Charset.defaultCharset());
    }
	
	@Bean
    public ChatMemoryRepository chatMemoryRepositoryOrchestrator() {
        return new InMemoryChatMemoryRepository();
    }
	
	@Bean
    public ChatMemory chatMemoryOrchestrator(@Qualifier("chatMemoryRepositoryOrchestrator") ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10)
                .build();
	}
	
	@Bean
    public ChatClient chatClientBuildTemplate(ChatClient.Builder builder, SaveHtmlDataTools saveHtmlDataToolsBean,
    		ImageSubAgentTools generateImageBean,
    		@Qualifier("chatMemoryOrchestrator") ChatMemory chatMemory) {
		MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();
        return builder
    		.clone()
    		.defaultTools(saveHtmlDataToolsBean, generateImageBean)
    		.defaultAdvisors(memoryAdvisor)
            .build();
    }
	
}
