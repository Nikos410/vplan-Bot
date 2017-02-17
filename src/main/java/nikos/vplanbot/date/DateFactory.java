package nikos.vplanbot.date;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class DateFactory {

    public static String date(String format){
        DateFormat dateFormat = new SimpleDateFormat(format); // Format setzen
        java.util.Date date = new java.util.Date();  // Datum auslesen

        return dateFormat.format(date);
    }
}
