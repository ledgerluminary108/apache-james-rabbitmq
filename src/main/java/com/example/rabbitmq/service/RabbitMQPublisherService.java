package com.example.rabbitmq.service;


import com.example.rabbitmq.config.RabbitMQExtensionConfiguration;
import com.example.rabbitmq.model.EmailActionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
@Singleton
public class RabbitMQPublisherService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQPublisherService.class);

    private final RabbitMQExtensionConfiguration config;
    private final ObjectMapper objectMapper;
    private Connection connection;
    private Channel channel;
    @Inject
    public RabbitMQPublisherService(RabbitMQExtensionConfiguration config) {
        this.config = config;
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

        // Declare the exchange
        channel.exchangeDeclare(config.getOutputExchange(), "direct", true);

        LOGGER.info("Started RabbitMQ publisher for exchange: {}", config.getOutputExchange());
    }

    public void publishResult(EmailActionResponse response) throws IOException {
        String message = objectMapper.writeValueAsString(response);

        channel.basicPublish(
                config.getOutputExchange(),
                config.getOutputRoutingKey(),
                null,
                message.getBytes(StandardCharsets.UTF_8)
        );

        LOGGER.info("Published result: {}", message);
    }

    public void stop() throws IOException, TimeoutException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
        LOGGER.info("Stopped RabbitMQ publisher");
    }
}