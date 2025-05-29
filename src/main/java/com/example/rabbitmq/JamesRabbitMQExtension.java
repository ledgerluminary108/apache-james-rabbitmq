package com.example.rabbitmq;


import com.example.rabbitmq.config.RabbitMQExtensionConfiguration;
import com.example.rabbitmq.service.RabbitMQConsumerService;
import com.example.rabbitmq.service.RabbitMQPublisherService;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@Singleton
public class JamesRabbitMQExtension {
    private static final Logger LOGGER = LoggerFactory.getLogger(JamesRabbitMQExtension.class);
    private final RabbitMQPublisherService publisherService;
    private final RabbitMQConsumerService consumerService;
    private final RabbitMQExtensionConfiguration config;
    private volatile boolean started = false;

    @Inject
    public JamesRabbitMQExtension(
            RabbitMQPublisherService publisherService,
            RabbitMQConsumerService consumerService,
            RabbitMQExtensionConfiguration config) {
        this.publisherService = publisherService;
        this.consumerService = consumerService;
        this.config = config;
        LOGGER.info("James RabbitMQ Extension - Constructor called");
        initializeAsync();
    }

    private void initializeAsync() {
        Thread initThread = new Thread(() -> {
            try {
                // Wait a bit for James to be ready
                Thread.sleep(10000); // 10 seconds
                initialize();
            } catch (Exception e) {
                LOGGER.error("Failed to initialize RabbitMQ Extension in async thread", e);
            }
        }, "RabbitMQ-Extension-Init");

        initThread.setDaemon(false); // Ensure thread completes
        initThread.start();
    }

    public void initialize() {
        if (started) {
            LOGGER.warn("RabbitMQ Extension already started");
            return;
        }

        try {
            LOGGER.info("Starting James RabbitMQ Extension...");

            // Initialize and start services
            publisherService.start();
            consumerService.start();

            started = true;
            LOGGER.info("James RabbitMQ Extension started successfully");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    cleanup();
                } catch (Exception e) {
                    System.err.println("❌ Failed to close RabbitMQ connection: " + e.getMessage());
                }
            }));
        } catch (Exception e) {
            LOGGER.error("Failed to start James RabbitMQ Extension", e);
            // Attempt cleanup on failure
            cleanup();
            throw new RuntimeException("Failed to initialize RabbitMQ Extension", e);
        }
    }


    private void cleanup() {
        started = false;

        if (consumerService != null) {
            try {
                consumerService.stop();
                LOGGER.debug("Consumer service stopped");
            } catch (Exception e) {
                LOGGER.error("Error stopping consumer service", e);
            }
        }

        if (publisherService != null) {
            try {
                publisherService.stop();
                LOGGER.debug("Publisher service stopped");
            } catch (Exception e) {
                LOGGER.error("Error stopping publisher service", e);
            }
        }
    }
}