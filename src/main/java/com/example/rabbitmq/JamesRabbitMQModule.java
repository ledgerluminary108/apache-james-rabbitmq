package com.example.rabbitmq;

import com.example.rabbitmq.config.RabbitMQExtensionConfiguration;
import com.example.rabbitmq.service.EmailManagementService;
import com.example.rabbitmq.service.RabbitMQConsumerService;
import com.example.rabbitmq.service.RabbitMQPublisherService;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class JamesRabbitMQModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(JamesRabbitMQModule.class);
    @Override
    protected void configure() {
        bind(RabbitMQExtensionConfiguration.class).toProvider(ConfigurationProvider.class).in(Singleton.class);
        bind(EmailManagementService.class).in(Singleton.class);
        bind(RabbitMQPublisherService.class).in(Singleton.class);
        bind(RabbitMQConsumerService.class).in(Singleton.class);
        // Eagerly instantiate the extension so @PostConstruct is called
        bind(JamesRabbitMQExtension.class).asEagerSingleton();
    }

    private static class ConfigurationProvider implements com.google.inject.Provider<RabbitMQExtensionConfiguration> {
        @Override
        public RabbitMQExtensionConfiguration get() {
            Properties props = loadPropertiesFile();
            RabbitMQExtensionConfiguration config = RabbitMQExtensionConfiguration.builder()
                    .host(getConfigValue(props, "rabbitmq.host", "localhost"))
                    .port(Integer.parseInt(getConfigValue(props, "rabbitmq.port", "5672")))
                    .username(getConfigValue(props, "rabbitmq.username", "guest"))
                    .password(getConfigValue(props, "rabbitmq.password", "guest"))
                    .virtualHost(getConfigValue(props, "rabbitmq.virtualHost", "/"))
                    .inputQueue(getConfigValue(props, "rabbitmq.inputQueue", "james.email.actions"))
                    .outputExchange(getConfigValue(props, "rabbitmq.outputExchange", "james.email.results"))
                    .outputRoutingKey(getConfigValue(props, "rabbitmq.outputRoutingKey", "result"))
                    .build();

            LOGGER.info("RabbitMQ Configuration loaded: host={}, port={}, username={}, queue={}",
                    config.getHost(), config.getPort(), config.getUsername(), config.getInputQueue());

            return config;
        }
        private Properties loadPropertiesFile() {
            Properties props = new Properties();

            // Try different possible locations for the properties file
            String[] possiblePaths = {
                    "/root/conf/rabbitmq.properties",
                    "conf/rabbitmq.properties",
                    "rabbitmq.properties",
                    System.getProperty("james.server.home", ".") + "/conf/rabbitmq.properties"
            };

            for (String path : possiblePaths) {
                try (InputStream is = new FileInputStream(path)) {
                    props.load(is);
                    LOGGER.info("Loaded RabbitMQ configuration from: {}", path);
                    break;
                } catch (Exception e) {
                    LOGGER.debug("Could not load from path: {}", path);
                }
            }

            // Try loading from classpath as well
            try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("rabbitmq.properties")) {
                if (is != null) {
                    props.load(is);
                    LOGGER.info("Loaded RabbitMQ configuration from classpath");
                }
            } catch (Exception e) {
                LOGGER.debug("Could not load from classpath", e);
            }

            return props;
        }

        private String getConfigValue(Properties props, String key, String defaultValue) {
            // Priority: 1. System property, 2. Properties file, 3. Environment variable, 4. Default
            String value = System.getProperty(key);
            if (value != null) {
                LOGGER.debug("Using system property for {}: {}", key, value);
                return value;
            }

            value = props.getProperty(key);
            if (value != null) {
                LOGGER.debug("Using properties file for {}: {}", key, value);
                return value;
            }

            // Try environment variable (convert dots to underscores and uppercase)
            String envKey = key.replace(".", "_").toUpperCase();
            value = System.getenv(envKey);
            if (value != null) {
                LOGGER.debug("Using environment variable {} for {}: {}", envKey, key, value);
                return value;
            }

            LOGGER.debug("Using default value for {}: {}", key, defaultValue);
            return defaultValue;
        }
    }
}