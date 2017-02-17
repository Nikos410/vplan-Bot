package nikos.vplanbot.notification;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import org.nikos.vplanbot.org.nikos.vplanbot.notification.util.MimeMessageFactory;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class GMailNotificationService implements NotificationService {
    private static final String APPLICATION_NAME = "Gmail API Java org.nikos.vplanbot.org.nikos.vplanbot.notification.SendMail";

    private static final Path DATA_STORE_DIR = Paths.get(System.getProperty("user.home"), ".credentials/gmail-java-sendmail");

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);

    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport HTTP_TRANSPORT;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR.toFile());
        } catch (GeneralSecurityException | IOException e) {
            System.err.println("Couldn't initialize GMailNotificationService. Reason: " + e.getLocalizedMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private final String sender;

    private final List<String> receiverAddresses;

    private final String notificationSubject;

    private final Path attachmentFile;

    private Gmail gmailService = getGmailService();

    public GMailNotificationService(
            final String sender,
            final List<String> receiverAddresses,
            final String notificationSubject,
            final Path attachmentFile) {
        this.sender = sender;
        this.receiverAddresses = receiverAddresses;
        this.notificationSubject = notificationSubject;
        this.attachmentFile = attachmentFile;
    }

    public void sendNotification(final String message) {
        try {
            //create Mail
            MimeMessage mimeMessageObj = MimeMessageFactory.createEmailWithAttachment(this.sender,
                    this.receiverAddresses,
                    this.notificationSubject,
                    message,
                    attachmentFile);

            //send Mail
            sendMessage(gmailService, "me", mimeMessageObj);
        } catch (MessagingException mex) {
            System.err.println("Error: MessagingException");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Gmail getGmailService() {
        Credential credential = authorize();
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private Credential authorize() {
        // Load client secrets.
        InputStream in = GMailNotificationService.class.getResourceAsStream("/client_secret.json");
        try {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow =
                    new GoogleAuthorizationCodeFlow.Builder(
                            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                            .setDataStoreFactory(DATA_STORE_FACTORY)
                            .setAccessType("offline")
                            .build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
            System.out.println("Credentials saved to " + DATA_STORE_DIR.toAbsolutePath().toString());
            return credential;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not authorize with GMail!", e);
        }
    }

    /**
     * Create a message from an email.
     *
     * @param emailContent Email to be set to raw of message
     * @return a message containing a base64url encoded email
     * @throws IOException
     * @throws MessagingException
     */
    private Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    /**
     * Send an email from the user's mailbox to its recipient.
     *
     * @param service      Authorized Gmail API instance.
     * @param userId       User's email address. The special value "me"
     *                     can be used to indicate the authenticated user.
     * @param emailContent Email to be sent.
     * @return The sent message
     * @throws MessagingException
     * @throws IOException
     */
    private Message sendMessage(Gmail service,
                                String userId,
                                MimeMessage emailContent)
            throws MessagingException, IOException {
        Message message = createMessageWithEmail(emailContent);
        message = service.users().messages().send(userId, message).execute();

        System.out.println("Message id: " + message.getId());
        System.out.println(message.toPrettyString());
        return message;
    }
}
