package nikos.vplanbot;

import nikos.vplanbot.checksum.ChecksumComparator;
import nikos.vplanbot.checksum.MD5FileDigestor;
import nikos.vplanbot.download.FileDownloader;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import nikos.vplanbot.date.DateFactory;
import nikos.vplanbot.notification.GMailNotificationService;
import nikos.vplanbot.notification.NotificationService;
import nikos.vplanbot.notification.TwitterNotificationService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;

/**
 * Danke an Sebastian K. !
 *
 * Zweck dieses Bots ist es, alle 10 Minuten den aktuellen Vertretungsplan herunterzuladen und zu überpruefen,
 * ob die Datei geändert wurde.
 * <p>
 * Wichtig: es muss sich eine Datei namens "vplan.pdf" im Verzeichnis plan befinden!
 * </p>
 */
public class Vplanbot {
    private static final Path configFile = Paths.get("config/config.ini");

    private final Path vplanFile;
    private final Path vplanTempFile;
    private final Path vplanArchiveDirectory;

    private final String vplanURL;

    private final String notificationMessage;
    private final String notificationSubject;

    private final String senderMailAddress;
    private final List<String> receiverMailAddresses;

    private NotificationService notificationServiceGMail;
    private NotificationService notificationServiceTwitter;

    private final int timerPeriod;
    private Timer timer;

    private Vplanbot(String[] timerPeriodArgs) {

        java.util.prefs.Preferences prefs = null;
        // ini Datei einlesen
        try {
            Ini ini = new Ini(configFile.toFile());
            prefs = new IniPreferences(ini);
        }
        catch (IOException e) {
            throw new RuntimeException("Could not read config file.", e);
        }

        // Pfade auslesen
        this.vplanFile = Paths.get(prefs.node("Paths").get("vplanFile", null));
        this.vplanTempFile = Paths.get(prefs.node("Paths").get("vplanTempFile", null));
        this.vplanArchiveDirectory = Paths.get(prefs.node("Paths").get("vplanArchiveDirectory",null));

        // Einstellungen auslesen
        String standardTimerPeriodString = prefs.node("Settings").get("standardTimerPeriod", null);
        final int standardTimerPeriod = Integer.parseInt(standardTimerPeriodString) * 1000;
        this.vplanURL = prefs.node("Settings").get("vplanURL", null);
        this.senderMailAddress = prefs.node("Settings").get("senderMailAddress", null);
        // Empfaenger Adressen auslesen und trennen
        String receiverMailAddressesString = prefs.node("Settings").get("receiverMailAddress", null);
        String[] receiverMailAddressesArray = receiverMailAddressesString.split(",");
        this.receiverMailAddresses = Arrays.asList(receiverMailAddressesArray);

        // Timer Periodendauer
        if(timerPeriodArgs.length != 0){
            // Es gibt ein Argument
            this.timerPeriod = (Integer.parseInt(timerPeriodArgs[0])*1000); // Angegebene Sekunden in die benötigten Millisekunden umwandeln
        }
        else {
            // Es gibt kein Argument -> Standard aus Config Datei verwenden
            this.timerPeriod = standardTimerPeriod;
        }

        // Nachricht und Betreff auslesen
        try {
            // Dateipfade auslesen
            final Path messageFile = Paths.get(prefs.node("Paths").get("messageFile", null));
            final Path subjectFile = Paths.get(prefs.node("Paths").get("subjectFile", null));

            // Nachricht auslesen
            final byte[] encodedMessage = Files.readAllBytes(messageFile);
            notificationMessage = new String(encodedMessage, StandardCharsets.UTF_8);

            // Betreff auslesen
            final byte[] encodedSubject =  Files.readAllBytes(subjectFile);
            this.notificationSubject = new String(encodedSubject, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new RuntimeException("Could not read config file.", e);
        }

        // Konfiguration ausgeben
        System.out.println( '\n' +
                "\u001B[36m" + "Config:" + "\u001B[0m" + '\n' +
                "\u001B[36m" + "Timer Periodendauer: " + "\u001B[0m" + this.timerPeriod/1000 + " Sekunden" + '\n' +
                "\u001B[36m" + "Archiv-Verzeichnis: " + "\u001B[0m" + this.vplanArchiveDirectory + '\n' +
                "\u001B[36m" + "Betreff: " + "\u001B[0m" + this.notificationSubject + '\n' +
                "\u001B[36m" + "Nachricht: " + "\u001B[0m" + this.notificationMessage + '\n' +
                "\u001B[36m" + "Absender Mail-Adresse: " + "\u001B[0m" + senderMailAddress + '\n' +
                "\u001B[36m" + "Empfänger Mail-Adresse(n): " + "\u001B[0m" + receiverMailAddresses);

        // GMail Benachrichtigung einrichten
        this.notificationServiceGMail = new GMailNotificationService(
                senderMailAddress,
                receiverMailAddresses,
                vplanFile);

        // Timer erstellen
        timer = new Timer();
    }

    private void run() {
        this.checkChange();

        // Timer erinrichten und starten
        timer.scheduleAtFixedRate(new Task(), timerPeriod, timerPeriod);

        // Programm laufen lassen bis es abgebrochen wird
        try {
            while (true) {
                Thread.sleep(1000);
            }
        }
        catch(InterruptedException e){
            System.err.println("Timer was interrupted: " + e.getLocalizedMessage());
        }
    }

    private class Task extends TimerTask {
        @Override
        public void run(){
            checkChange();
        }
    }

    private void checkChange(){
        System.out.println('\n' + "Vertretungsplan wird überprüft! " + DateFactory.date("[dd.MM. - HH:mm:ss]"));

        downloadFile(vplanURL, vplanTempFile);

        final List<byte[]> checksums = MD5FileDigestor.digestFiles(Arrays.asList(vplanFile, vplanTempFile));

        if (ChecksumComparator.compareChecksums(checksums)) {
            // Beide Dateien sind gleich -> Der Plan nicht veraendert.
            System.out.println("\u001B[33m" + "Keine Aenderung gefunden!" + "\u001B[0m");

            // Heruntergeladenen Plan loeschen, da er nicht anders ist
            try {
                Files.delete(vplanTempFile);
                System.out.println("\u001B[33m" + "Temporaere Datei geloescht." + "\u001B[0m");
            } catch (IOException e) {
                final String errorMessage = MessageFormat.format("Could not delete file {0}. Reason: {1}", vplanTempFile, e.getLocalizedMessage());
                System.err.println(errorMessage);
                e.printStackTrace();
            }
        }
        else {
            // Die Dateien sind unterschiedlich -> Der Plan wurde veraendert
            System.out.println("\u001B[36m" + "Aenderung gefunden!" + "\u001B[0m");

            // Alten Plan löschen und durch neuen ersetzen
            replaceFile(vplanFile, vplanTempFile);
            // Neuen Plan mit Timestamp versehen und ins Archiv kopieren
            archiveFile(vplanFile, vplanArchiveDirectory);

            System.out.println("\u001B[34m" + vplanFile.getFileName() + "\u001B[36m" + " wurde aktualisiert." + "\u001B[0m" + '\n');

            this.newNotification();
        }
    }

    private void newNotification() {
        System.out.println("\u001B[36m" + "Benachrichtigungen werden versendet." + "\u001B[0m");

        // neue Twitter-Benachrichtigung erstellen
        this.notificationServiceTwitter = new TwitterNotificationService();

        // aktuelles Datum an die Nachricht anhängen
        final String dateNotificationMessage = this.notificationMessage + " " + '\n' + DateFactory.date("[dd.MM. - HH:mm]");

        // Twitter Benachrichtung schicken
        notificationServiceTwitter.sendNotification(dateNotificationMessage);
        System.out.println('\n' + "\u001B[34m" + "Twitter" + "\u001B[36m" + " Benachrichtigung verschickt." + "\u001B[0m");

        // Mail Benachrichtigung schicken
        notificationServiceGMail.sendNotification(dateNotificationMessage, this.notificationSubject);
        System.out.println("\u001B[34m" + "E-Mail" + "\u001B[36m" + " Benachrichtigung verschickt." + "\u001B[0m");
    }

    private static Path archiveFile(Path file, Path archiveDirectory) {
        String date = DateFactory.date("dd.MM.YYYY_HH.mm");
        Path archivedFile = Paths.get(archiveDirectory.toString() + "/" + date + ".pdf");

        try {
            Files.copy(file,archivedFile);
        }
        catch (IOException e) {
            throw new RuntimeException("Could not copy file " + file.getFileName(), e);
        }
        return archivedFile;
    }

    private static void replaceFile(Path existingFile, Path newFile) {
        try {
            Files.delete(existingFile);
            Files.move(newFile, newFile.resolveSibling(existingFile.getFileName()));
        } catch (IOException e) {
            final String errorMessage = MessageFormat.format("Could not replace file {0} with file {1}. Reason: {2}", existingFile, newFile, e.getLocalizedMessage());
            System.err.println(errorMessage);
            e.printStackTrace();
        }
    }

    private static void downloadFile(String url, Path savePath) {
        try {
            final URL fileUrl = new URL(url);
            FileDownloader.download(fileUrl, savePath);
        } catch (MalformedURLException e) {
            throw new RuntimeException("URL is malformed: " + url, e);
        } catch (IOException e) {
            throw new RuntimeException("Could not download file at URL " + url, e);
        }
    }

    public static void main(String[] args) {
        // Argumente: Timer-Periodendauer in Sekunden
        new Vplanbot(args).run();
    }
}
