package org.nikos.vplanbot;

/**
 * Danke an Sebastian K. !
 *
 * Zweck dieses Bots ist es, alle 10 Minuten den aktuellen Vertretungsplan herunterzuladen und zu überpruefen,
 * ob die Datei geändert wurde.
 * <p>
 * Wichtig: es muss sich eine Datei namens "vplan.pdf" im Verzeichnis befinden!
 * </p>
 */

import org.nikos.vplanbot.org.nikos.vplanbot.checksum.ChecksumComparator;
import org.nikos.vplanbot.org.nikos.vplanbot.checksum.MD5FileDigestor;
import org.nikos.vplanbot.org.nikos.vplanbot.date.DateReader;
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
import java.util.*;

public class Vplanbot {

    private Timer timer;
    private static final int standardTimerPeriod = 600000; //600000 -> 600 Sekunden -> 10 Minuten
    private final int timerPeriod;

    private final String notificationMessage;
    private final String notificationSubject;

    private final String senderMailAddress = "lightningcrafter410@gmail.com";
    private final List<String> receiverMailAddresses = Collections.singletonList("nikos.epping@web.de");

    private NotificationService notificationServiceGMail;
    private NotificationService notificationServiceTwitter;

    private Vplanbot(String[] timerPeriodArgs) {

        if(timerPeriodArgs.length != 0){
            //Es gibt ein Argument
            timerPeriod = (Integer.parseInt(timerPeriodArgs[0])*1000); //Angegebene Sekunden in die benötigten Millisekunden umwandeln
        }
        else {
            //Es gibt kein Argument -> Standard verwenden
            timerPeriod = standardTimerPeriod;
        }

        try {
            final byte[] encodedSubject =  Files.readAllBytes(Paths.get("config/subject.txt"));   //Betreff auslesen
            this.notificationSubject = new String(encodedSubject, StandardCharsets.UTF_8);

            final byte[] encodedMessage = Files.readAllBytes(Paths.get("config/message.txt"));    //Nachricht auslesen
            notificationMessage = new String(encodedMessage, StandardCharsets.UTF_8);

            //GMail Benachrichtigung einrichten
            this.notificationServiceGMail = new GMailNotificationService(
                    senderMailAddress,
                    receiverMailAddresses,
                    Paths.get("vplan.pdf"));

            //Konfiguration ausgeben
            System.out.println( '\n' +
                                "\u001B[36m" + "Config:" + "\u001B[0m" + '\n' +
                                "\u001B[36m" + "Timer Periodendauer: " + "\u001B[0m" + timerPeriod/1000 + " Sekunden" + '\n' +
                                "\u001B[36m" + "Betreff: " + "\u001B[0m" + this.notificationSubject + '\n' +
                                "\u001B[36m" + "Nachricht: " + "\u001B[0m" + this.notificationMessage + '\n' +
                                "\u001B[36m" + "Absender Mail-Adresse: " + "\u001B[0m" + senderMailAddress + '\n' +
                                "\u001B[36m" + "Empfänger Mail-Adresse(n): " + "\u001B[0m" + receiverMailAddresses);

        } catch (IOException e) {
            throw new RuntimeException("Could not readArea subject and/or message files.", e);
        }

        //Timer erstellen
        timer = new Timer();
    }

    private void run() {

        this.checkChange();

        //Timer erinrichten und starten
        timer.scheduleAtFixedRate(new Task(), timerPeriod, timerPeriod);

        try {
            while (true) {
                Thread.sleep(1000);
            }
        }
        catch(InterruptedException e){
            System.err.println("InterruptedException: " + e.getLocalizedMessage());
        }
    }

    private class Task extends TimerTask {
        @Override public void run(){
            checkChange();
        }
    }

    private void checkChange(){
        System.out.println('\n' + "Vertretungsplan wird überprüft! " + DateReader.date("[dd.MM. - HH:mm:ss]"));

        final Path existingFile = Paths.get("vplan.pdf");
        final Path newFile = downloadFile("http://kapu-bocholt.de/plan/vplan.pdf");

        final List<byte[]> checksums = MD5FileDigestor.digestFiles(Arrays.asList(existingFile, newFile));

        if (ChecksumComparator.compareChecksums(checksums)) {
            //Beide Dateien sind gleich -> Der Plan nicht veraendert.
            System.out.println("\u001B[33m" + "Keine Aenderung gefunden!" + "\u001B[0m");

            //Heruntergeladenen Plan loeschen, da er nicht anders ist
            try {
                Files.delete(newFile);
                System.out.println("\u001B[33m" + "Temporaere Datei geloescht." + "\u001B[0m");
            } catch (IOException e) {
                final String errorMessage = MessageFormat.format("Could not delete file {0}. Reason: {1}", newFile, e.getLocalizedMessage());
                System.err.println(errorMessage);
                e.printStackTrace();
            }
        } else {
            //Die Dateien sind unterschiedlich -> Der Plan wurde veraendert
            System.out.println("\u001B[36m" + "Aenderung gefunden!" + "\u001B[0m");
            replaceFile(existingFile, newFile);

            newNotification();
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

    private void newNotification() {
        System.out.println("\u001B[36m" + "Benachrichtigungen werden versendet." + "\u001B[0m");

        //neue Twitter-Benachrichtigung erstellen
        this.notificationServiceTwitter = new TwitterNotificationService();

        //aktuelles Datum an die Nachricht anhängen
        final String dateNotificationMessage = this.notificationMessage + " " + '\n' + DateReader.date("[dd.MM. - HH:mm]");

        //Twitter Benachrichtung abschicken
        notificationServiceTwitter.sendNotification(dateNotificationMessage);
        System.out.println('\n' + "\u001B[36m" + "Twitter Benachrichtigung verschickt." + "\u001B[0m" + '\n');

        //Mail Benachrichtigung abschicken
        notificationServiceGMail.sendNotification(dateNotificationMessage, this.notificationSubject);
        System.out.println("\u001B[36m" + "E-Mail Benachrichtigung verschickt." + "\u001B[0m");
    }

    private Path downloadFile(String url) {
        final String fileToDownload = url;
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
        //Argumente: Timer-Periodendauer in Sekunden
        new Vplanbot(args).run();
    }
}
