# Apache James RabbitMQ Listener

This module is a Java-based RabbitMQ listener designed to integrate with Apache James. It listens for action requests on a queue, processes JSON messages, and sends a result status to a response queue.

---

## 🚀 Current Features

- Connects to RabbitMQ via `localhost:5672`
- Listens on `james-actions` queue
- Parses incoming JSON messages:
  ```json
  {
    "action": "move",
    "sourceMailboxID": "user1@example.com",
    "sourceMessageID": "msg123",
    "destinationMailboxID": "user2@example.com",
    "hashID": "test001"
  }
  ```
- Logs action and prepares for mailbox operation
- Sends result to `james-results` queue:
  ```json
  {
    "hashID": "test001",
    "status": "success"
  }
  ```

---

## 🛠️ Prerequisites

- Java 11+
- Maven
- RabbitMQ running locally or via Docker:
  ```bash
  docker run -d --hostname my-rabbit --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
  ```

Access RabbitMQ UI at [http://localhost:15672](http://localhost:15672)  
Login: `guest` / `guest`

---

## 🔧 Setup & Run

### Build the project:

```bash
mvn clean compile
```

### Run the listener:

```bash
mvn exec:java -Dexec.mainClass="com.example.rabbitmq.RabbitMQListener"
```

You should see logs like:

```
[Init] Starting RabbitMQListener...
[Receive] {...}
[Process] ...
[Send] {"hashID":"...","status":"success"}
```

---

## 🧪 Test a Message

Use RabbitMQ Web UI or CLI to send this to `james-actions`:

```json
{
  "action": "move",
  "sourceMailboxID": "user1@example.com",
  "sourceMessageID": "msg123",
  "destinationMailboxID": "user2@example.com",
  "hashID": "test001"
}
```

Check `james-results` queue for:

```json
{ "hashID": "test001", "status": "success" }
```

---

## 🧩 Next Steps

- Integrate with Apache James `MailboxManager`
- Locate and move/trash messages based on input
- Add error handling for mailbox failures
- Prepare extension JAR for deployment

---

## 📁 File Structure

```
src/
└── main/
    └── java/
        └── com/
            └── example/
                └── rabbitmq/
                    └── RabbitMQListener.java
```

---

## 📝 License

MIT License
