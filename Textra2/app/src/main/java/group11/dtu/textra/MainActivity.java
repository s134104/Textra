package group11.dtu.textra;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<Contact> contacts = new ArrayList<Contact>();

    //  GUI Widget
    ListView lvMsg;

    // Cursor Adapter
    CustomAdapter adapter;

    final int flag = 0; // 0 = Avg for all messages, overrides rangeInHours. 1 = Avg from today to rangeInHours. 2 = Avg from latest date in messages to rangeInHours
    final int rangeInHours = 168; // Compute averages for the past 7 days

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Read all text messages and processing
        firstRun();

        lvMsg = (ListView) findViewById(R.id.lvMsg);
        adapter = new CustomAdapter(this, getContacts());
        lvMsg.setAdapter(adapter);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.export:
                String fileName = "log_data.txt";

                try {
                    File root = new File(Environment.getExternalStorageDirectory() + File.separator + "Textra", "Logs");
                    //File root = new File(Environment.getExternalStorageDirectory(), "Notes");
                    if (!root.exists()) {
                        Log.i("MainActivity", "Creating dir @" + Environment.getExternalStorageDirectory() + File.separator + "Textra" + File.separator + "Logs");
                        root.mkdirs();
                    }
                    File gpxfile = new File(root, fileName);
                    if(gpxfile.delete()){
                        gpxfile.createNewFile();
                    }
                    FileWriter writer = new FileWriter(gpxfile, true);

                    for(int i = 0; i < getContacts().size(); i++) {
                        //Log.i("MainActivity", "" + getContacts().get(i).getContactName(getApplicationContext()) + ", IN: " + convertToTime(getContacts().get(i).computeAverageRTIn()) + ", OUT: " + convertToTime(getContacts().get(i).computeAverageRTOut()));
                        writer.append("PERSON" + i + ", IN: " + convertToTime(getContacts().get(i).computeAverageRTIn(flag, rangeInHours)) + ", OUT: " + convertToTime(getContacts().get(i).computeAverageRTOut(flag, rangeInHours)) + "\n");
                        writer.flush();
                    }

                    writer.flush();
                    writer.close();

                } catch (IOException e) {
                    e.printStackTrace();

                }


                Toast.makeText(getApplicationContext(), "Exported", Toast.LENGTH_LONG).show();

                return true;
            case R.id.about:
                Toast.makeText(getApplicationContext(), "About", Toast.LENGTH_LONG).show();
                return true;
            case R.id.help:
                Toast.makeText(getApplicationContext(), "Help", Toast.LENGTH_LONG).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Populate the custom listView accordingly
    private class CustomAdapter extends BaseAdapter {
        Context context;
        ArrayList<Contact> getContacts;

        public CustomAdapter(MainActivity activity, ArrayList<Contact> getContacts) {

            this.context = activity;
            this.getContacts = getContacts;
        }

        @Override
        public int getCount() {
            return getContacts.size();
        }

        @Override
        public Object getItem(int i) {
            return i;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            View v = view;

            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.row, null);

            }

            TextView contactName  = (TextView)v.findViewById(R.id.contactName);
            TextView contactIn  = (TextView)v.findViewById(R.id.contactIn);
            TextView contactOut = (TextView)v.findViewById(R.id.contactOut);

            // Look up contact name in phonebook from number
            String getName = getContacts.get(i).getContactName(getApplicationContext());

            // If number not in phone book, display number
            if(getName == null)
                getName = getContacts.get(i).getContactNumber();

            // Get initials for privacy
            StringBuilder builder = new StringBuilder(3);
            for(int j = 0; j < getName.split(" ").length; j++) {
                builder.append(getName.split(" ")[j].substring(0, 1) + ". ");
            }

            contactName.setText(builder.toString());
            contactIn.setText("" + convertToTime(getContacts.get(i).computeAverageRTIn(flag, rangeInHours)));
            contactOut.setText("" + convertToTime(getContacts.get(i).computeAverageRTOut(flag, rangeInHours)));

            return v;
        }
    }

    public void firstRun() {
        Uri message = Uri.parse("content://sms/");
        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(message, null, null, null, null);
        startManagingCursor(c);
        int totalSMS = c.getCount();

        String contactNumber;

        if (c.moveToFirst()) {
            for (int i = 0; i < totalSMS - 1; i++) {
                // Shift through all received text messages
                contactNumber = c.getString(c.getColumnIndexOrThrow("address"));

                if(contactNumber.substring(0, 1).equals("+"))
                    // Remove country number prefix. Need to add 00-prefix too.
                    contactNumber = contactNumber.substring(3, contactNumber.length());

                if(getContact(contactNumber) == null) {
                    Contact newContact = new Contact(contactNumber);
                    newContact.addMessages(c.getLong(c.getColumnIndex("date")), c.getString(c.getColumnIndexOrThrow("type")));
                    contacts.add(newContact);
                } else {
                    getContact(contactNumber).addMessages(c.getLong(c.getColumnIndex("date")), c.getString(c.getColumnIndexOrThrow("type")));
                }

                c.moveToNext();
            }

        }

        //Log.i("MainActivity", "GET RESPONSE TIMES OUT: " +  getContact("PHONENUMBERHERE").computeResponseTimesOut().toString());
        //Log.i("MainActivity", "GET AVERAGE RESPONSE TIMES OUT: " +  convertToTime(getContact("PHONENUMBERHERE").computeAverageRTOut(flag, rangeInHours)));

        /*
        Log.i("MainActivity", "GET NAME FROM NUMBER: " + getContact("PHONENUMBERHERE").getContactName(getApplicationContext()) );
        Log.i("MainActivity", "GET NUMBER OF MESSAGES TO X: " +  getContact("PHONENUMBERHERE").getMessages().size() );
        Log.i("MainActivity", "GET MESSAGE DATE TO X: " +  getContact("PHONENUMBERHERE").getMessages().get(7).getDate() + ", TYPE: " + getContact("PHONENUMBERHERE").getMessages().get(7).getType() );

        for(int i = 0; i < getContact("61730299").getMessages().size(); i++) {
            Log.i("MainActivity", "GET DATES: " + getContact("PHONENUMBERHERE").getMessages().get(i).getDate() + ", TYPE: " + getContact("PHONENUMBERHERE").getMessages().get(i).getType());
        }

        Log.i("MainActivity", "GET NUMBER OF CONTACTS: " +  getContacts().size());
        Log.i("MainActivity", "GET COMPUTED RESPONSE TIMES IN: " +  getContact("PHONENUMBERHERE").computeResponseTimesIn().size());
        Log.i("MainActivity", "GET COMPUTED RESPONSE TIMES OUT: " +  getContact("PHONENUMBERHERE").computeResponseTimesOut().size());
        Log.i("MainActivity", "GET RESPONSE TIMES IN: " +  getContact("PHONENUMBERHERE").computeResponseTimesIn().toString());
        Log.i("MainActivity", "GET RESPONSE TIMES OUT: " +  getContact("PHONENUMBERHERE").computeResponseTimesOut().toString());
        Log.i("MainActivity", "GET AVERAGE RESPONSE TIMES IN: " +  getContact("PHONENUMBERHERE").computeAverageRTIn());
        Log.i("MainActivity", "GET AVERAGE RESPONSE TIMES OUT: " +  getContact("PHONENUMBERHERE").computeAverageRTOut());
        */

        // Shawn's phone
        /*for(int i = 0; i < getContacts().size(); i++) {
            Log.i("MainActivity", "GET NAME: " + getContacts().get(i).getContactName(getApplicationContext()));
        }
        Log.i("MainActivity", "GET NUMBER OF CONTACTS: " +  getContacts().size());
        Log.i("MainActivity", "GET NUMBER OF TEXTS: " + c.getCount());
        */

    }

    public ArrayList<Contact> getContacts() { return contacts; }
    public Contact getContact(String contactNumber) {
        if(!contacts.isEmpty()) {
            for(Contact contact : contacts) {
                if(contact.getContactNumber().equals(contactNumber)) {
                    return contact;
                }
            }
        } else {
            Log.i("MainActivity", "No such contact.");
        }

        return null;
    }

    public String convertToTime(int averageInSeconds) {
        int hours = averageInSeconds / 3600;
        int minutes = (averageInSeconds % 3600) / 60;
        int seconds = averageInSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

}

