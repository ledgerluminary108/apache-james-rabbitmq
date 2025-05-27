package com.example.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import org.apache.james.core.Username;
import org.apache.james.lifecycle.api.Startable;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageRange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class RabbitMQListener implements Startable {

    private static final String QUEUE_NAME = "james-actions";
    private static final String RESULT_QUEUE = "james-results";

    private final MailboxManager mailboxManager;

    public RabbitMQListener(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }

    @Override
    public void start() {
        System.out.println("[Init] Starting RabbitMQListener...");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");

        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            channel.queueDeclare(RESULT_QUEUE, false, false, false, null);

            channel.basicConsume(QUEUE_NAME, true, (consumerTag, message) -> {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                System.out.println("[Receive] " + body);

                String result;
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, String> payload = objectMapper.readValue(body, Map.class);
                    result = processPayload(payload);
                } catch (Exception e) {
                    System.err.println("[Error] Failed to parse or process message:");
                    e.printStackTrace();
                    result = "{\"hashID\":\"unknown\",\"status\":\"failed\"}";
                }

                channel.basicPublish("", RESULT_QUEUE, null, result.getBytes(StandardCharsets.UTF_8));
                System.out.println("[Send] " + result);

            }, consumerTag -> {
                // no-op
            });

            // Block to keep the listener alive
            System.out.println("[Listener] Waiting for messages. Press Ctrl+C to stop.");
            synchronized (this) {
                this.wait();
            }

        } catch (IOException | TimeoutException | InterruptedException e) {
            System.err.println("[Fatal] Listener error:");
            e.printStackTrace();
        }

    }

    private String processPayload(Map<String, String> payload) {
        String hashID = payload.getOrDefault("hashID", "unknown");
        String action = payload.get("action");
        String src = payload.get("sourceMailboxID");
        String msg = payload.get("sourceMessageID");
        String dst = payload.get("destinationMailboxID");

        if (mailboxManager == null) {
            System.err.println("[Fatal] MailboxManager is null.");
            return String.format("{\"hashID\":\"%s\",\"status\":\"failed\"}", hashID);
        }

        try {
            Username sourceUser = Username.of(src);
            MailboxSession session = mailboxManager.createSystemSession(sourceUser);
            mailboxManager.startProcessingRequest(session);

            MailboxPath sourcePath = MailboxPath.forUser(sourceUser, "INBOX");
            MessageManager sourceManager = mailboxManager.getMailbox(sourcePath, session);
            MessageUid messageUid = MessageUid.of(Long.parseLong(msg));

            switch (action.toLowerCase()) {
                case "trash":
                    MailboxPath trashPath = MailboxPath.forUser(sourceUser, "Trash");
                    mailboxManager.moveMessages(
                            MessageRange.one(messageUid),
                            sourcePath,
                            trashPath,
                            session);
                    break;

                case "move":
                    Username destUser = Username.of(dst);
                    MailboxPath destPath = MailboxPath.forUser(destUser, "INBOX");
                    mailboxManager.moveMessages(
                            MessageRange.one(messageUid),
                            sourcePath,
                            destPath,
                            session);
                    break;

                default:
                    System.err.printf("[Warn] Unsupported action '%s' in payload%n", action);
                    return String.format("{\"hashID\":\"%s\",\"status\":\"failed\"}", hashID);
            }

            mailboxManager.endProcessingRequest(session);
            return String.format("{\"hashID\":\"%s\",\"status\":\"success\"}", hashID);

        } catch (Exception e) {
            System.err.printf("[Mailbox Error] hashID=%s | Cause: %s: %s%n",
                    hashID, e.getClass().getSimpleName(), e.getMessage());
            return String.format("{\"hashID\":\"%s\",\"status\":\"failed\"}", hashID);
        }
    }

    public static void main(String[] args) {
        System.out.println("[Main] Launching RabbitMQListener (mock mode)");
        // Note: this will NOT process actual mailbox logic unless wired into James
        RabbitMQListener listener = new RabbitMQListener(null);
        listener.start();
    }
}
