/**
 * Zweck dieses Bots ist es, den aktuellen Vertretungsplan herunterzuladen und zu ueberpruefen,
 * ob die Datei in den letzten 10 Minuten geaendert wurde.
 *
 * Wichtig: es muss sich eine Datei namens "vplan.pdf" im Verzeichnis befinden!
 */
import twitter4j.TwitterException;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.NoSuchFileException;
import java.nio.file.DirectoryNotEmptyException;
import java.security.MessageDigest;
import java.util.*;

public class VplanBot {
    public static void main(String[] args) {

        //Download des aktuellen Vertretungsplans
        Download();

        //Vergleichen der beiden Dateien per Checksum
        Checksum();
    }

    private static void Download() {
        //Diese Methode laedt den aktuellen Vertretungsplan herunter und speichert ihn ab.
        System.out.println("Verbindung aufgebaut.");
        try {
            URL url = new URL(File URL);
            InputStream in = url.openStream();
            OutputStream fos = new FileOutputStream("vplanTEMP");

            int length = -1;

            byte[] buffer = new byte[1024];

            while ((length = in.read(buffer)) > -1) {

                fos.write(buffer, 0, length);

            }
            System.out.println("Verbindung getrennt.");
            fos.close();

            in.close();
        } catch (java.net.MalformedURLException e) {
            System.err.println("Error: Vermutlich soll von einer ungueltigen URL heruntergeladen werden (MalformedURLException)");
        } catch (java.io.IOException e) {
            System.err.println("IOException");
        }

        System.out.println('\n' + "Aktueller Vertretungsplan heruntergeladen." + '\n');
    }

    private static void Checksum() {
        //Diese Methode vergleicht die Checksums der beiden Dateien
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            final byte[] fileBytes1 = Files.readAllBytes(Paths.get("vplan.pdf"));
            final byte[] checksum1 = md.digest(fileBytes1);

            md.reset();

            final byte[] fileBytes2 = Files.readAllBytes(Paths.get("vplanTEMP"));
            final byte[] checksum2 = md.digest(fileBytes2);

            if (Arrays.equals(checksum1, checksum2)) {

                //Die Dateien sind gleich -> Der Plan wurde nicht verändert
                SameAction();

            } else {

                //Die Dateien sind unterschiedlich -> Der Plan wurde veraendert
                ChangeAction();

            }

        } catch (java.security.NoSuchAlgorithmException e) {
            System.err.println("Error: NoSuchAlgorithmException");
        } catch (java.io.IOException e) {
            System.err.println("Error: Vermutlich ist keine Datei mit dem Namen 'vplan.pdf' im Verzeichnis oder der Plan wurde nicht richtig (mit dem Namen 'vplanTEMP')heruntergeladen. (IOException)");
        }
    }

    private static void Delete(String name) {
        //Diese Methode loescht eine Datei.
        try {
            Files.delete(Paths.get(name));
        } catch (NoSuchFileException e) {
            System.err.println("Error: Vermutlich existiert keine Datei mit dem Namen '" + name + "' (IOException)");
        } catch (DirectoryNotEmptyException e) {
            System.err.println("Error: Vermutlich wird versucht ein nicht leeres Verzeichnis zu löschen (DirectoryNotEmptyException)");
        } catch (IOException e) {
            System.err.println("Error: IOException");
        }
    }

    private static void Rename(String altName, String neuName) {
        //Diese Methode aendert den Namen einer Datei von altName.pdf zu neuName.pdf
        try {
            Path source = Paths.get(altName);
            Files.move(source, source.resolveSibling(neuName));
        } catch (java.io.IOException e) {
            System.err.println("Error: IOException");
        }
    }

    private static void SameAction() {

        System.out.println("\u001B[33m" + "Keine Aenderung gefunden!");

        //Temporäre Datei loeschen
        Delete("vplanTEMP");

        System.out.println("Temporäre Datei gelöscht.");

    }

    private static void ChangeAction() {

        System.out.println("\u001B[36m" + "Aenderung gefunden!");

        //Alten, nicht aktuellen Plan loeschen
        Delete("vplan.pdf");

        //temporaeren Plan umbennennen -> alten Plan aktualisieren
        Rename("vplanTEMP", "vplan.pdf");
        System.out.println("\u001B[34m" + "vplan.pdf" + "\u001B[36m" + " aktualisiert." + "\u001B[0m" + '\n');

        //E-Mail Benachrichtigung
        try {
            SendMail newMail = new SendMail("receiver@web.de");
            System.out.println('\n' + "\u001B[36m" + "E-Mail Benachrichtigung verschickt.");

        } catch (IOException e) {
            System.err.println("Error: IOException");
        }

        //Tweet Benachrichtigung
        /*Tweet tweet = new Tweet("Neuer Vertretungsplan liegt vor!");
        System.out.println('\n' + "\u001B[36m" + "Tweet verschickt.");
        */


    }

}
