#!/bin/bash

# Create necessary directories
mkdir -p extensions-jars
mkdir -p conf

# Build the extension
echo "Building James RabbitMQ Extension..."
mvn clean package

# Copy the jar to extensions-jars
cp target/james-extension-rabbitmq-1.0-SNAPSHOT.jar extensions-jars/

# Create extensions.properties if it doesn't exist
if [ ! -f conf/extensions.properties ]; then
    echo "Creating extensions.properties..."
    cat > conf/extensions.properties << EOF
# James RabbitMQ Extension
guice.extension.module=com.example.rabbitmq.JamesRabbitMQModule
EOF
fi

# Create rabbitmq.properties if it doesn't exist
if [ ! -f conf/rabbitmq.properties ]; then
    echo "Creating rabbitmq.properties..."
    cat > conf/rabbitmq.properties << EOF
# RabbitMQ Extension Configuration
rabbitmq.host=rabbitmq
rabbitmq.port=5672
rabbitmq.username=james
rabbitmq.password=secret
rabbitmq.virtualHost=/
rabbitmq.inputQueue=james.email.actions
rabbitmq.outputExchange=james.email.results
rabbitmq.outputRoutingKey=result
EOF
fi

echo "Setup complete!"
echo "Now run: docker-compose up -d"
