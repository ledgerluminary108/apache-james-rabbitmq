package com.example.rabbitmq.config;

public class RabbitMQExtensionConfiguration {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String virtualHost;
    private final String inputQueue;
    private final String outputExchange;
    private final String outputRoutingKey;

    private RabbitMQExtensionConfiguration(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.username = builder.username;
        this.password = builder.password;
        this.virtualHost = builder.virtualHost;
        this.inputQueue = builder.inputQueue;
        this.outputExchange = builder.outputExchange;
        this.outputRoutingKey = builder.outputRoutingKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String host = "localhost";
        private int port = 5672;
        private String username = "guest";
        private String password = "guest";
        private String virtualHost = "/";
        private String inputQueue = "james.email.actions";
        private String outputExchange = "james.email.results";
        private String outputRoutingKey = "result";

        public Builder host(String host) { this.host = host; return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder virtualHost(String virtualHost) { this.virtualHost = virtualHost; return this; }
        public Builder inputQueue(String inputQueue) { this.inputQueue = inputQueue; return this; }
        public Builder outputExchange(String outputExchange) { this.outputExchange = outputExchange; return this; }
        public Builder outputRoutingKey(String outputRoutingKey) { this.outputRoutingKey = outputRoutingKey; return this; }

        public RabbitMQExtensionConfiguration build() {
            return new RabbitMQExtensionConfiguration(this);
        }
    }

    // Getters
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getVirtualHost() { return virtualHost; }
    public String getInputQueue() { return inputQueue; }
    public String getOutputExchange() { return outputExchange; }
    public String getOutputRoutingKey() { return outputRoutingKey; }
}