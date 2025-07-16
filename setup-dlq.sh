#!/bin/bash

# =============================================================================
# Kafka DLQ Setup and Troubleshooting Script
# =============================================================================

echo "🚀 Starting Kafka DLQ Setup..."

# Step 1: Clean up any existing containers
echo "🧹 Cleaning up existing containers..."
docker-compose down -v
docker system prune -f

# Step 2: Start services with proper order
echo "📦 Starting services..."
docker-compose up -d zookeeper

# Wait for Zookeeper to be ready
echo "⏳ Waiting for Zookeeper to be ready..."
sleep 15

# Check Zookeeper health
echo "🔍 Checking Zookeeper health..."
docker exec zookeeper sh -c 'echo ruok | nc localhost 2181'

# Start Kafka
echo "📦 Starting Kafka..."
docker-compose up -d kafka

# Wait for Kafka to be ready
echo "⏳ Waiting for Kafka to be ready..."
sleep 30

# Check Kafka health
echo "🔍 Checking Kafka health..."
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Create required topics
echo "📝 Creating Kafka topics..."

# Main payment events topic
docker exec kafka kafka-topics --create \
  --topic payment-events \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

# DLQ topic
docker exec kafka kafka-topics --create \
  --topic payment-events-dlq \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

# Retry topic
docker exec kafka kafka-topics --create \
  --topic payment-events-retry \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

# Start RabbitMQ
echo "📦 Starting RabbitMQ..."
docker-compose up -d rabbitmq

# Wait for RabbitMQ to be ready
echo "⏳ Waiting for RabbitMQ to be ready..."
sleep 15

# Start Kafka UI
echo "📦 Starting Kafka UI..."
docker-compose up -d kafka-ui

# Wait for Kafka UI to be ready
echo "⏳ Waiting for Kafka UI to be ready..."
sleep 10

# Verify all services are running
echo "🔍 Verifying services..."
docker-compose ps

# Test Kafka connectivity
echo "🧪 Testing Kafka connectivity..."
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Test RabbitMQ connectivity
echo "🧪 Testing RabbitMQ connectivity..."
docker exec rabbitmq rabbitmqctl status

echo "✅ Setup complete!"
echo ""
echo "📊 Service URLs:"
echo "   - Kafka UI: http://localhost:8080"
echo "   - RabbitMQ Management: http://localhost:15672 (guest/guest)"
echo ""
echo "🔧 Useful commands:"
echo "   - View Kafka topics: docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list"
echo "   - View topic details: docker exec kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic payment-events"
echo "   - Consume messages: docker exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic payment-events --from-beginning"
echo "   - Produce test message: docker exec kafka kafka-console-producer --bootstrap-server localhost:9092 --topic payment-events"
echo ""

# =============================================================================
# Troubleshooting Functions
# =============================================================================

troubleshoot_kafka() {
    echo "🔧 Troubleshooting Kafka..."

    # Check if Kafka container is running
    if ! docker ps | grep -q kafka; then
        echo "❌ Kafka container is not running"
        return 1
    fi

    # Check Kafka logs
    echo "📋 Kafka logs (last 50 lines):"
    docker logs kafka --tail 50

    # Check if Kafka is listening on the expected port
    echo "🔍 Checking Kafka ports..."
    docker exec kafka ss -tuln | grep 9092

    # Test Kafka broker connectivity
    echo "🧪 Testing Kafka broker connectivity..."
    docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092

    # List existing topics
    echo "📋 Existing topics:"
    docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list
}

troubleshoot_zookeeper() {
    echo "🔧 Troubleshooting Zookeeper..."

    # Check if Zookeeper container is running
    if ! docker ps | grep -q zookeeper; then
        echo "❌ Zookeeper container is not running"
        return 1
    fi

    # Check Zookeeper logs
    echo "📋 Zookeeper logs (last 50 lines):"
    docker logs zookeeper --tail 50

    # Test Zookeeper connectivity
    echo "🧪 Testing Zookeeper connectivity..."
    docker exec zookeeper sh -c 'echo ruok | nc localhost 2181'
}

troubleshoot_rabbitmq() {
    echo "🔧 Troubleshooting RabbitMQ..."

    # Check if RabbitMQ container is running
    if ! docker ps | grep -q rabbitmq; then
        echo "❌ RabbitMQ container is not running"
        return 1
    fi

    # Check RabbitMQ logs
    echo "📋 RabbitMQ logs (last 50 lines):"
    docker logs rabbitmq --tail 50

    # Test RabbitMQ connectivity
    echo "🧪 Testing RabbitMQ connectivity..."
    docker exec rabbitmq rabbitmqctl status

    # List queues
    echo "📋 RabbitMQ queues:"
    docker exec rabbitmq rabbitmqctl list_queues
}

# =============================================================================
# Manual Topic Creation (if auto-creation fails)
# =============================================================================

create_topics_manually() {
    echo "📝 Creating topics manually..."

    # Wait for Kafka to be ready
    echo "⏳ Waiting for Kafka to be ready..."
    while ! docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1; do
        echo "Waiting for Kafka..."
        sleep 5
    done

    # Create topics one by one
    echo "Creating payment-events topic..."
    docker exec kafka kafka-topics --create \
        --topic payment-events \
        --bootstrap-server localhost:9092 \
        --partitions 3 \
        --replication-factor 1 \
        --config retention.ms=604800000 \
        --if-not-exists

    echo "Creating payment-events-dlq topic..."
    docker exec kafka kafka-topics --create \
        --topic payment-events-dlq \
        --bootstrap-server localhost:9092 \
        --partitions 3 \
        --replication-factor 1 \
        --config retention.ms=2592000000 \
        --if-not-exists

    echo "Creating payment-events-retry topic..."
    docker exec kafka kafka-topics --create \
        --topic payment-events-retry \
        --bootstrap-server localhost:9092 \
        --partitions 3 \
        --replication-factor 1 \
        --config retention.ms=86400000 \
        --if-not-exists

    # Verify topics were created
    echo "✅ Topics created successfully:"
    docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list
}

# =============================================================================
# Test Message Production and Consumption
# =============================================================================

test_kafka_dlq() {
    echo "🧪 Testing Kafka DLQ functionality..."

    # Create a test consumer for main topic
    echo "📥 Starting consumer for main topic (in background)..."
    docker exec kafka kafka-console-consumer \
        --bootstrap-server localhost:9092 \
        --topic payment-events \
        --from-beginning \
        --timeout-ms 5000 &

    # Create a test consumer for DLQ topic
    echo "📥 Starting consumer for DLQ topic (in background)..."
    docker exec kafka kafka-console-consumer \
        --bootstrap-server localhost:9092 \
        --topic payment-events-dlq \
        --from-beginning \
        --timeout-ms 5000 &

    # Send test messages
    echo "📤 Sending test messages..."
    echo '{"paymentId":"test-123","userId":"user-456","amount":100.00,"currency":"USD"}' | \
        docker exec -i kafka kafka-console-producer \
            --bootstrap-server localhost:9092 \
            --topic payment-events

    echo "✅ Test messages sent. Check consumers above for output."
}

# =============================================================================
# RabbitMQ Queue Setup
# =============================================================================

setup_rabbitmq_queues() {
    echo "📝 Setting up RabbitMQ queues..."

    # Wait for RabbitMQ to be ready
    while ! docker exec rabbitmq rabbitmqctl status > /dev/null 2>&1; do
        echo "Waiting for RabbitMQ..."
        sleep 5
    done

    # Create exchanges
    docker exec rabbitmq rabbitmqctl eval '
        application:start(rabbit),
        rabbit_exchange:declare({resource, <<"/">>, exchange, <<"payments.exchange">>}, direct, true, false, false, []),
        rabbit_exchange:declare({resource, <<"/">>, exchange, <<"dlx.payments">>}, direct, true, false, false, []).
    '

    # Create queues with DLQ configuration
    docker exec rabbitmq rabbitmqctl eval '
        application:start(rabbit),
        rabbit_amqqueue:declare({resource, <<"/">>, queue, <<"payment.queue">>}, true, false, [
            {<<"x-dead-letter-exchange">>, longstr, <<"dlx.payments">>},
            {<<"x-dead-letter-routing-key">>, longstr, <<"payment.failed">>},
            {<<"x-message-ttl">>, long, 60000}
        ], none),
        rabbit_amqqueue:declare({resource, <<"/">>, queue, <<"payment.dlq">>}, true, false, [], none).
    '

    echo "✅ RabbitMQ queues created successfully"
}

# =============================================================================
# Usage Instructions
# =============================================================================

show_usage() {
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  setup              - Full setup (default)"
    echo "  troubleshoot-kafka - Troubleshoot Kafka issues"
    echo "  troubleshoot-zk    - Troubleshoot Zookeeper issues"
    echo "  troubleshoot-rmq   - Troubleshoot RabbitMQ issues"
    echo "  create-topics      - Manually create Kafka topics"
    echo "  test-kafka         - Test Kafka DLQ functionality"
    echo "  setup-rabbitmq     - Setup RabbitMQ queues"
    echo "  help               - Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                 # Run full setup"
    echo "  $0 troubleshoot-kafka"
    echo "  $0 create-topics"
}

# =============================================================================
# Main Execution
# =============================================================================

case "${1:-setup}" in
    "setup")
        # Full setup is already done above
        ;;
    "troubleshoot-kafka")
        troubleshoot_kafka
        ;;
    "troubleshoot-zk")
        troubleshoot_zookeeper
        ;;
    "troubleshoot-rmq")
        troubleshoot_rabbitmq
        ;;
    "create-topics")
        create_topics_manually
        ;;
    "test-kafka")
        test_kafka_dlq
        ;;
    "setup-rabbitmq")
        setup_rabbitmq_queues
        ;;
    "help")
        show_usage
        ;;
    *)
        echo "Unknown command: $1"
        show_usage
        exit 1
        ;;
esac