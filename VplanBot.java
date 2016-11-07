package org.nikos.vplanbot;
/**
 * Danke an Sebastian K. !
 *
 * Zweck dieses Bots ist es, den aktuellen Vertretungsplan herunterzuladen und zu ueberpruefen,
 * ob die Datei in den letzten 10 Minuten geaendert wurde.
 * <p>
 * Wichtig: es muss sich eine Datei namens "vplan.pdf" im Verzeichnis befinden!
 * </p>
 */

import org.nikos.vplanbot.org.nikos.vplanbot.checksum.ChecksumComparator;
import org.nikos.vplanbot.org.nikos.vplanbot.checksum.MD5FileDigestor;
import org.nikos.vplanbot.org.nikos.vplanbot.download.FileDownloader;
import org.nikos.vplanbot.org.nikos.vplanbot.notification.GMailNotificationService;
import org.nikos.vplanbot.org.nikos.vplanbot.notification.NotificationService;
import org.nikos.vplanbot.org.nikos.vplanbot.notification.TwitterNotificationService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Date;

public class Vplanbot {

    private String notificationMessage;

    private final NotificationService notificationServiceGMail;
    private final NotificationService notificationServiceTwitter;

    private Vplanbot() {
        try {
            byte[] encodedSubject =  Files.readAllBytes(Paths.get("config/subject.txt"));   //Betreff auslesen
            final String subject = new String(encodedSubject, StandardCharsets.UTF_8);

            byte[] encodedMessage = Files.readAllBytes(Paths.get("config/message.txt"));    //Nachricht auslesen
            this.notificationMessage = new String(encodedMessage, StandardCharsets.UTF_8);

            DateFormat dateFormat = new SimpleDateFormat("[dd.MM - HH:mm]"); //[07.11. - 17:49]
            Date date = new Date();
            this.notificationMessage = notificationMessage + " " + '\n' + (dateFormat.format(date));

            final String senderMailAddress = "sender@gmail.com";
            final List<String> receiverMailAddresses = Collections.singletonList("receiver@whatever.de");

            this.notificationServiceGMail = new GMailNotificationService(
                    senderMailAddress,
                    receiverMailAddresses,
                    subject,
                    Paths.get("vplan.pdf"));

            this.notificationServiceTwitter = new TwitterNotificationService();

        } catch (IOException e) {
            throw new RuntimeException("Could not read subject and/or message files.", e);
        }
    }

    private void run() {
        final Path existingFile = Paths.get("vplan.pdf");
        final Path newFile = downloadFile();

        final List<byte[]> checksums = MD5FileDigestor.digestFiles(Arrays.asList(existingFile, newFile));

        if (ChecksumComparator.compareChecksums(checksums)) {
            //Beide Dateien sind gleich -> Der Plan nicht veraendert.
            System.out.println('\n' + "\u001B[33m" + "Keine Aenderung gefunden!");

            //Heruntergeladenen Plan loeschen, da er nicht anders ist
            try {
                Files.delete(newFile);
                System.out.println("Temporaere Datei geloescht.");
            } catch (IOException e) {
                final String errorMessage = MessageFormat.format("Could not delete file {0}. Reason: {1}", newFile, e.getLocalizedMessage());
                System.err.println(errorMessage);
                e.printStackTrace();
            }
        } else {
            //Die Dateien sind unterschiedlich -> Der Plan wurde veraendert
            System.out.println('\n' + "\u001B[36m" + "Aenderung gefunden!");
            replaceFile(existingFile, newFile);

            //Mail Benachrichtigung
            notificationServiceGMail.sendNotification(this.notificationMessage);
            System.out.println('\n' + "\u001B[36m" + "E-Mail Benachrichtigung verschickt.");

            notificationServiceTwitter.sendNotification(this.notificationMessage);
            System.out.println('\n' + "\u001B[36m" + "Twitter Benachrichtigung verschickt.");
        }
    }

    private void replaceFile(Path existingFile, Path newFile) {
        //Alten, nicht aktuellen Plan loeschen
        try {
            Files.delete(existingFile);
            Files.move(newFile, newFile.resolveSibling(existingFile.getFileName()));
        } catch (IOException e) {
            final String errorMessage = MessageFormat.format("Could not replace file {0} with file {1}. Reason: {2}", existingFile, newFile, e.getLocalizedMessage());
            System.err.println(errorMessage);
            e.printStackTrace();
        }
        System.out.println("\u001B[34m" + "vplan.pdf" + "\u001B[36m" + " aktualisiert." + "\u001B[0m" + '\n');
    }

    private Path downloadFile() {
        final String fileToDownload = "URL to vplan.pdf";
        try {
            final URL fileUrl = new URL(fileToDownload);
            final FileDownloader downloader = new FileDownloader(fileUrl, Paths.get("vplanTEMP.pdf"));
            return downloader.download();
        } catch (MalformedURLException e) {
            throw new RuntimeException("URL is malformed: " + fileToDownload, e);
        } catch (IOException e) {
            throw new RuntimeException("Could not download file at URL " + fileToDownload, e);
        }
    }

    public static void main(String[] args) {
        new Vplanbot().run();
    }
}
