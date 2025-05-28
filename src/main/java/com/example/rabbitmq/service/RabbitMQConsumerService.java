package com.example.rabbitmq.service;


import com.example.rabbitmq.config.RabbitMQExtensionConfiguration;
import com.example.rabbitmq.model.EmailActionRequest;
import com.example.rabbitmq.model.EmailActionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
@Singleton
public class RabbitMQConsumerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQConsumerService.class);

    private final RabbitMQExtensionConfiguration config;
    private final EmailManagementService emailService;
    private final RabbitMQPublisherService publisherService;
    private final ObjectMapper objectMapper;
    private Connection connection;
    private Channel channel;
    @Inject
    public RabbitMQConsumerService(RabbitMQExtensionConfiguration config,
                                   EmailManagementService emailService,
                                   RabbitMQPublisherService publisherService) {
        this.config = config;
        this.emailService = emailService;
        this.publisherService = publisherService;
        this.objectMapper = new ObjectMapper();
    }

    public void start() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.getHost());
        factory.setPort(config.getPort());
        factory.setUsername(config.getUsername());
        factory.setPassword(config.getPassword());
        factory.setVirtualHost(config.getVirtualHost());

        connection = factory.newConnection();
        channel = connection.createChannel();

        // Declare the queue
        channel.queueDeclare(config.getInputQueue(), true, false, false, null);

        LOGGER.info("Started RabbitMQ consumer for queue: {}", config.getInputQueue());

        // Set up message consumer
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            LOGGER.info("Received message: {}", message);

            try {
                EmailActionRequest request = objectMapper.readValue(message, EmailActionRequest.class);
                EmailActionResponse response = emailService.processEmailAction(request);

                // Publish result
                publisherService.publishResult(response);

                // Acknowledge message
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

            } catch (Exception e) {
                LOGGER.error("Error processing message: {}", message, e);
                // Reject and requeue the message
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
            }
        };

        channel.basicConsume(config.getInputQueue(), false, deliverCallback, consumerTag -> {});
    }

    public void stop() throws IOException, TimeoutException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
        LOGGER.info("Stopped RabbitMQ consumer");
    }
}