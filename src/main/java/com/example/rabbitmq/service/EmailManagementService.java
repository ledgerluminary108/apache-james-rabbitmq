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
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.jpa.JPAId;
import org.apache.james.mailbox.model.Mailbox;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MailboxMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@Singleton
public class EmailManagementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailManagementService.class);

    private final Provider<MailboxManager> mailboxManagerProvider;
    private final Provider<MailboxMapperFactory> mailboxMapperFactoryProvider;

    @Inject
    public EmailManagementService(Provider<MailboxManager> mailboxManagerProvider,
                                  Provider<MailboxMapperFactory> mailboxMapperFactoryProvider) {
        this.mailboxManagerProvider = mailboxManagerProvider;
        this.mailboxMapperFactoryProvider = mailboxMapperFactoryProvider;
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
            MailboxMapper mailboxMapper = getMailboxMapper(mailboxManager);
            Mailbox sourceMailBox = mailboxMapper.findMailboxById(parseMailboxID(request.getSourceMailboxID())).block();
            if (sourceMailBox == null) {
                throw new MailboxException("Source mailbox not found");
            }
            Username username = sourceMailBox.getUser();
            mailboxManager.startProcessingRequest(session);

            session = mailboxManager.createSystemSession(username);
            MailboxId sourceMailBoxId = sourceMailBox.getMailboxId();

            Mailbox desitnationMailBox = mailboxMapper.findMailboxById(parseMailboxID(request.getDestinationMailboxID())).block();
            if (desitnationMailBox == null) {
                throw new MailboxException("Destination mailbox not found");
            }
            MailboxId destinationMailBoxId = desitnationMailBox.getMailboxId();

            MessageUid messageUid = MessageUid.of(Long.parseLong(request.getSourceMessageID()));
            LOGGER.info("Start move mail {} from source {} to dest {}", messageUid, request.getSourceMailboxID(),
                    request.getDestinationMailboxID());
            mailboxManager.moveMessages(
                    MessageRange.one(messageUid),
                    sourceMailBoxId,
                    destinationMailBoxId,
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

    private EmailActionResponse handleTrashAction(EmailActionRequest request) {
        MailboxSession session = null;
        MailboxManager mailboxManager = mailboxManagerProvider.get();
        try {
            MailboxMapper mailboxMapper = getMailboxMapper(mailboxManager);
            Mailbox sourceMailBox = mailboxMapper.findMailboxById(parseMailboxID(request.getSourceMailboxID())).block();
            if (sourceMailBox == null) {
                throw new MailboxException("Source mailbox not found");
            }
            Username username = sourceMailBox.getUser();
            session = mailboxManager.createSystemSession(username);
            MailboxId sourceMailBoxId = parseMailboxID(request.getSourceMailboxID());
            MailboxPath sourcePath = mailboxManager.getMailbox(sourceMailBoxId, session).getMailboxPath();
            mailboxManager.startProcessingRequest(session);
            MessageUid messageUid = MessageUid.of(Long.parseLong(request.getSourceMessageID()));
            MailboxPath trashPath = MailboxPath.forUser(sourcePath.getUser(), "Trash");
            boolean mailboxExists = Boolean.TRUE.equals(Mono.from(mailboxManager.mailboxExists(trashPath, session)).block());
            if (!mailboxExists) {
                mailboxManager.createMailbox(trashPath, session);
            }
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

    private MailboxMapper getMailboxMapper(MailboxManager mailboxManager) throws MailboxException {
        return mailboxMapperFactoryProvider.get()
                .getMailboxMapper(mailboxManager.createSystemSession(Username.of("james-rabbitmq-extension")));
    }

    private MailboxId parseMailboxID(String mailboxID) {
        return new JPAId(Long.parseLong(mailboxID));
    }
}
