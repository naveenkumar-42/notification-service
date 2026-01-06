package com.notification;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class NotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Bean
	public CommandLineRunner purgeQueuesOnStartup() {
		return args -> {
			try {
				log.warn("🧹 Purging queues on startup...");
				rabbitTemplate.execute(channel -> {
					channel.queuePurge("notification.queue");
					channel.queuePurge("notification.dlq");
					return null;
				});
				log.info("✅ Queues purged successfully");
			} catch (Exception e) {
				log.error("❌ Failed to purge queues: {}", e.getMessage());
			}
		};
	}
}

