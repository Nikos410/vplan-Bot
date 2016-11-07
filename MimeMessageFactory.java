package org.nikos.vplanbot.org.nikos.vplanbot.notification.util;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public class MimeMessageFactory {

    /**
     * Create a MimeMessage using the parameters provided.
     *
     * @param subject  Subject of the email.
     * @param bodyText Body text of the email.
     * @param attachmentFilePath     Path to the file to be attached.
     * @return MimeMessage to be used to send email.
     * @throws MessagingException
     */
    public static MimeMessage createEmailWithAttachment(String sender, List<String> receivers, String subject, String bodyText, Path attachmentFilePath)
            throws MessagingException, IOException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(sender));
        for (String receiver : receivers) {
            email.addRecipient(Message.RecipientType.TO, new InternetAddress(receiver));

        }
        email.setSubject(subject);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(bodyText, "text/plain");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        mimeBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(attachmentFilePath.toFile());

        mimeBodyPart.setDataHandler(new DataHandler(source));
        mimeBodyPart.setFileName(attachmentFilePath.getFileName().toString());

        multipart.addBodyPart(mimeBodyPart);
        email.setContent(multipart);

        return email;
    }
}
