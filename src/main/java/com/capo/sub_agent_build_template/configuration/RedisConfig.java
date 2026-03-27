package com.capo.sub_agent_build_template.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.capo.sub_agent_build_template.services.RedisReceiverService;


@Configuration
public class RedisConfig {
	
	@Bean
    ChannelTopic requestTopic() {
        return new ChannelTopic("playwright-request");
    }
	
	@Bean
    ChannelTopic responseTopic() {
        return new ChannelTopic("playwright-response");
    }
	
	@Bean
    MessageListenerAdapter messageListener(RedisReceiverService receiver) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(receiver, "receiveMessage");
        adapter.setSerializer(new GenericJackson2JsonRedisSerializer());
        return adapter;
    }
	
	@Bean
    RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory,
                                                  MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, responseTopic());
        return container;
    }
	
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
	    RedisTemplate<String, Object> template = new RedisTemplate<>();
	    template.setConnectionFactory(connectionFactory);
	    GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
	    template.setKeySerializer(new StringRedisSerializer());
	    template.setValueSerializer(serializer);
	    template.setHashValueSerializer(serializer);
	    return template;
	}
}
