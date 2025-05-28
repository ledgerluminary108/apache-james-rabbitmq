package com.example.rabbitmq.service;


import com.example.rabbitmq.model.EmailAction;
import com.example.rabbitmq.model.EmailActionRequest;
import com.example.rabbitmq.model.EmailActionResponse;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.apache.james.core.Username;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Singleton
public class EmailManagementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailManagementService.class);

    private final Provider<MailboxManager> mailboxManagerProvider;

    @Inject
    public EmailManagementService(Provider<MailboxManager> mailboxManagerProvider) {
        this.mailboxManagerProvider = mailboxManagerProvider;
        LOGGER.info("EmailManagementService initialized with Provider<MailboxManager>");
    }

    public EmailActionResponse processEmailAction(EmailActionRequest request) {
        try {
            LOGGER.info("Processing email action: {}", request);

            EmailAction action = EmailAction.fromString(request.getAction());

            switch (action) {
                case MOVE:
                    return handleMoveAction(request);
                case TRASH:
                    return handleTrashAction(request);
                default:
                    return EmailActionResponse.failure(request.getHashID(),
                            "Unsupported action: " + request.getAction());
            }
        } catch (Exception e) {
            LOGGER.error("Error processing email action: {}", request, e);
            return EmailActionResponse.failure(request.getHashID(),
                    "Error processing action: " + e.getMessage());
        }
    }

    private EmailActionResponse handleMoveAction(EmailActionRequest request) {
        if (request.getDestinationMailboxID() == null) {
            return EmailActionResponse.failure(request.getHashID(),
                    "Destination mailbox ID is required for move action");
        }
        MailboxSession session = null;
        MailboxManager mailboxManager = mailboxManagerProvider.get();
        try {
            LOGGER.info("Start processing request session");
            MailboxPath sourcePath = parseMailboxID(request.getSourceMailboxID())
                    .orElseThrow(() -> new IllegalArgumentException("Source mailbox ID is not found with " + request.getSourceMailboxID()));
            MailboxPath destPath = parseMailboxID(request.getDestinationMailboxID())
                    .orElseThrow(() -> new IllegalArgumentException("Destination mailbox is not found with " + request.getDestinationMailboxID()));
            session = mailboxManager.createSystemSession(sourcePath.getUser());
            mailboxManager.startProcessingRequest(session);
            MessageUid messageUid = MessageUid.of(Long.parseLong(request.getSourceMessageID()));
            LOGGER.info("Start move mail {} from source {} to dest {}", messageUid, request.getSourceMailboxID(),
                    request.getDestinationMailboxID());
            mailboxManager.moveMessages(
                    MessageRange.one(messageUid),
                    sourcePath,
                    destPath,
                    session);
            // Parse message UID
            LOGGER.info("Successfully moved message {} from mailbox {} to mailbox {}",
                    request.getSourceMessageID(), request.getSourceMailboxID(), request.getDestinationMailboxID());
            return EmailActionResponse.success(request.getHashID(),
                    "Message successfully moved to destination mailbox");

        } catch (Exception e) {
            LOGGER.error("Error in move action", e);
            return EmailActionResponse.failure(request.getHashID(),
                    "Error processing move action: " + e.getMessage());
        } finally {
            if (session != null) {
                mailboxManager.endProcessingRequest(session);
            }
        }
    }

    private MailboxPath getSourceMailbox(EmailActionRequest request, Username sourceUser) {
        // Get source mailbox
        LOGGER.info("Get mailbox ID: {}", request.getSourceMailboxID());
        return MailboxPath.forUser(sourceUser, "INBOX");
    }

    private EmailActionResponse handleTrashAction(EmailActionRequest request) {
        MailboxSession session = null;
        MailboxManager mailboxManager = mailboxManagerProvider.get();
        try {
            MailboxPath sourcePath = parseMailboxID(request.getSourceMailboxID())
                    .orElseThrow(() -> new IllegalArgumentException("Source mailbox ID is not found with " + request.getSourceMailboxID()));
            session = mailboxManager.createSystemSession(sourcePath.getUser());
            mailboxManager.startProcessingRequest(session);
            MessageUid messageUid = MessageUid.of(Long.parseLong(request.getSourceMessageID()));
            MailboxPath trashPath = MailboxPath.forUser(sourcePath.getUser(), "TRASH");
            mailboxManager.moveMessages(
                    MessageRange.one(messageUid),
                    sourcePath,
                    trashPath,
                    session);
            LOGGER.info("Successfully trashed message {} from mailbox {}",
                    request.getSourceMessageID(), request.getSourceMailboxID());
            return EmailActionResponse.success(request.getHashID(),
                    "Message successfully moved to trash");

        } catch (Exception e) {
            LOGGER.error("Error in trash action", e);
            return EmailActionResponse.failure(request.getHashID(),
                    "Error processing trash action: " + e.getMessage());
        } finally {
            if (session != null) {
                mailboxManager.endProcessingRequest(session);
            }
        }
    }

    private Optional<MailboxPath> parseMailboxID(String mailboxID) {
        return MailboxPath.parseEscaped(mailboxID);
    }
}
