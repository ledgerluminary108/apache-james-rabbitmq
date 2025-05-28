#!/usr/bin/env python3
import pika
import json
import time

def create_test_cases():
    """Create test cases with correct mailbox ID format"""

    test_cases = [
        {
            "action": "TRASH",
            "sourceMailboxID": "#private:testuser@localhost:INBOX",
            "sourceMessageID": "1",
            "destinationMailboxID": None,
            "hashID": "test-trash-" + str(int(time.time()))
        },
        {
            "action": "MOVE",
            "sourceMailboxID": "#private:testuser@localhost:INBOX",
            "sourceMessageID": "2",
            "destinationMailboxID": "#private:testuser@localhost:Archive",
            "hashID": "test-move-" + str(int(time.time()))
        },
        # {
        #     "action": "MOVE",
        #     "sourceMailboxID": "#private:testuser@localhost:INBOX",
        #     "sourceMessageID": "3",
        #     "destinationMailboxID": "#private:testuser@localhost:Work",
        #     "hashID": "test-move-work-" + str(int(time.time()))
        # },
        # {
        #     "action": "FLAG",
        #     "sourceMailboxID": "#private:testuser@localhost:INBOX",
        #     "sourceMessageID": "4",
        #     "destinationMailboxID": None,
        #     "hashID": "test-flag-" + str(int(time.time())),
        #     "flagName": "Important"
        # },
        # {
        #     "action": "MARK_READ",
        #     "sourceMailboxID": "#private:testuser@localhost:INBOX",
        #     "sourceMessageID": "5",
        #     "destinationMailboxID": None,
        #     "hashID": "test-read-" + str(int(time.time()))
        # }
    ]

    return test_cases

def send_test_messages():
    """Send test messages with correct format"""

    try:
        # Connect to RabbitMQ
        credentials = pika.PlainCredentials('james', 'secret')
        connection = pika.BlockingConnection(
            pika.ConnectionParameters('localhost', 5672, '/', credentials)
        )
        channel = connection.channel()

        # Ensure queue exists
        channel.queue_declare(queue='james.email.actions', durable=True)

        test_cases = create_test_cases()

        print("=== Sending Test Messages with Correct Format ===\n")

        for i, test_case in enumerate(test_cases, 1):
            print(f"Test {i}: {test_case['action']}")
            print(f"Message: {json.dumps(test_case, indent=2)}")

            # Send message
            channel.basic_publish(
                exchange='',
                routing_key='james.email.actions',
                body=json.dumps(test_case),
                properties=pika.BasicProperties(delivery_mode=2)
            )

            print(f"✓ Sent successfully\n")
            time.sleep(2)  # Delay between messages

        connection.close()
        print(f"✓ All {len(test_cases)} test messages sent!")
        print("\nCheck James logs with:")
        print("docker-compose logs -f james | grep -E '(★|Processing|mailbox|Moving)'")

    except Exception as e:
        print(f"✗ Failed to send test messages: {e}")

if __name__ == "__main__":
    send_test_messages()
