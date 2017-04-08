package group11.dtu.textra;

import android.text.format.DateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by mohammad on 25/03/2017.
 */

public class Messages {
    private String address;
    private Long date;
    private String type; // 1 = In, 2 = Out

    public Messages(String address, Long date, String type) {
        this.address = address;
        this.date = date;
        this.type = type;
    }

    public String getDirection() {
        if(getType().equals("1"))
            return getAddress();

        return "Me";
    }
    public String getAddress() { return address; }
    public Long getTimestamp() { return date; }
    public String getDate() {
        // Convert timestamp to date format
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(date);
        return DateFormat.format("dd-MM-yyyy HH:mm:ss", cal).toString();
    }
    public Calendar getDateInstance() {
        // Convert timestamp to date format
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(date);
        return cal;
    }
    public String getType() { return type; }



}
