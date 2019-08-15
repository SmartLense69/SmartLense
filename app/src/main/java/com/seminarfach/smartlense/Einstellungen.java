package com.seminarfach.smartlense;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Choosing between WLAN and Bluetooth and getting several network address, given by user
 * @author Marc Beling
 */
@SuppressWarnings("ALL")
public class Einstellungen extends AppCompatActivity {

    //Tag for logcat
    public final String TAG = "Einstellungen.java";

    //Bluetooth Stuff
    //BT Socket is required for Camera Activity
    public static Boolean wifioderbluetooth = true;
    public static String IPaddress;
    public static OutputStream outputStream;
    public InputStream inputStream;
    public String BTAdress = "B8:27:EB:E6:8A:FD";
    public String userMAC = null;
    public boolean mEnablingBT = false;
    public boolean BTSocketconnected;
    public boolean failure = false;
    public TextView IP;

    //Progressbar Stuff
    private ProgressBar progressBar;
    private int progressStatus = 0;
    private Handler handler = new Handler();

    //startActivityforResult has to be in a method block

    /**
     * startActivityforResult, but in a method block
     * @param enableIntent      Intent: dialog window
     * @param REQUEST_ENABLE_BT Integer: can be anything beyond 0
     */
    public void mStartActivityforResult(Intent enableIntent, int REQUEST_ENABLE_BT) {
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    /**
     * Pops up Dialog Window, if User doesn't want to use Bluetooth
     */
    public void finishDialogNoBluetooth() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You cannot use this application without bluetooth.")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.app_name)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Enables Bluetooth on Device. Pops up Dialog Window to request User to enable Bluetooth
     * @param bluetoothAdapter Bluetooth Adapter: Get it with getDefaultAdapter()
     */
    public void enableBT(BluetoothAdapter bluetoothAdapter) {
        if (!bluetoothAdapter.isEnabled()) {
            //Create an intent with the ACTION_REQUEST_ENABLE action, which we’ll use to display
            //our system Activity
            //Pass this intent to startActivityForResult(). ENABLE_BT_REQUEST_CODE is a
            //locally defined integer that must be greater than 0,
            //for example private static final int ENABLE_BT_REQUEST_CODE = 1
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("This Application needs Bluetooth. Do you want to turn it on?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Warning")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mEnablingBT = true;
                            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            mStartActivityforResult(enableIntent, 2);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finishDialogNoBluetooth();
                        }
                    });
            Log.v(TAG, "Bluetooth is already enabled");
        }
    }

    //Some Methods for finalBTConnect has to be always active, so BluetoothConnector Class is
    //constructed in MainActivity.

    /**
     * Wrapped Bluetooth Connection Method, in order to save space for <b>real</b> necessary stuff.
     * @param mItemsPairedDevices Array Adapter: List of paired devices
     * @param mBTAddress          String: MAC Address of the certain device
     * @param mListView ListView for the paired devices
     * @param mTextView TextView for the error message
     * @param mUserMAC String: Given MAC-Address by user via EditView
     * @return Bluetooth Socket: Later used for connection
     */
    public BluetoothSocket finalBTConnect(ArrayAdapter mItemsPairedDevices,
                                          String mBTAddress,
                                          ListView mListView,
                                          TextView mTextView,
                                          String mUserMAC) {

        //Create Bluetooth Adapter
        BluetoothAdapter mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        //Check if succeed
        if (mBTAdapter == null) {
            Log.v(TAG, "FAIL: Could not create Bluetooth Adapter");
        } else {
            Log.v(TAG, "Bluetooth Adapter created");
        }

        //Enable Bluetooth on Device when necessary
        enableBT(mBTAdapter);
        //Get Bluetooth Devices
        BluetoothDevice BTdevice =
                MainActivity.bluetoothConnector.
                        getBTpaired(mBTAdapter, mItemsPairedDevices, mBTAddress);
        if (BTdevice == null) {
            if (mUserMAC == null) {
                Log.v(TAG, "Raspberry Pi not found!");
                Toast.makeText(this, "Your Raspberry wasn't found.", Toast.LENGTH_SHORT).show();
                //Suggests User to connect with an already paired device.
                mListView.setVisibility(View.VISIBLE);
                mTextView.setVisibility(View.VISIBLE);
                return null;
            } else {
                BTdevice = MainActivity.bluetoothConnector.
                        getBTpaired(mBTAdapter, mItemsPairedDevices, mUserMAC);
            }
        } else {
            mListView.setVisibility(View.INVISIBLE);
            mTextView.setVisibility(View.INVISIBLE);
        }

        //In order to connect successfully, a Discovery Request must be sent
        MainActivity.bluetoothConnector.sendDiscoverRequest(mBTAdapter);
        Log.v(TAG, "Discovery Request started");
        mBTAdapter.cancelDiscovery();

        Log.v(TAG, "Discovery canceled. (Usual)");

        //Create a Bluetooth Socket
        //NOTE: Due to Android 4.2.1, Method createRFcommSocket() doesn't work properly
        //Using hidden Method instead.
        //Have a look: https://stackoverflow.com/a/25647197
        try {
            MainActivity.BTsocket =
                    (BluetoothSocket) BTdevice.getClass()
                            .getMethod("createRfcommSocket",
                                    new Class[]{int.class}).invoke(BTdevice, 1);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            //if Bluetooth is disabled.
            e.printStackTrace();
            Log.v(TAG, "Bluetooth is disabled. Couldn't create BTSocket");
            MainActivity.BTsocket = null;
        }
        return MainActivity.BTsocket;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //Button for Bluetooth Connection
        final Button bluetooth_connect = findViewById(R.id.bluetooth_connect);
        final Button wlan_connect = findViewById(R.id.wlan_connect);
        final EditText SSIDedeittext = findViewById(R.id.SSIDedittext);
        final EditText PSKedittext = findViewById(R.id.PSKedittext);
        EditText BluetoothAddress = findViewById(R.id.MACedittext);
        //String BTAdress = BluetoothAddress.getText().toString();

        final TextView IP = findViewById(R.id.IP);
        IP.setVisibility(View.INVISIBLE);
        final TextView bluetoothmessage = findViewById(R.id.bluetoothmessage);
        IP.setVisibility(View.INVISIBLE);

        //Create ListView for paired Devices.
        // Not neccessary to display, but required for Method finalBTConnect
        final ArrayAdapter<String> itemsPairedDevices =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        final ListView listView = findViewById(R.id.pairedDevices);
        listView.setAdapter(itemsPairedDevices);
        listView.setVisibility(View.INVISIBLE);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected item text from ListView
            }
        });

        final TextView textView = findViewById(R.id.textView7);
        textView.setVisibility(View.INVISIBLE);

        //Switch for WLAN / Bluetooth
        Switch s = findViewById(R.id.switch1);
        wifioderbluetooth = s.isChecked();

        bluetooth_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Checks for Device Ability for Bluetooth
                if (MainActivity.bluetoothConnector.checkBT()) {

                    //Prevent creating multiple Sockets
                    if (MainActivity.BTsocket == null) {

                        MainActivity.BTsocket = finalBTConnect(itemsPairedDevices,
                                BTAdress, listView, textView, userMAC);
                    }
                    if (MainActivity.BTsocket == null) {
                        Log.v(TAG, "FAIL: Bluetooth probably not enabled");

                    } else {
                        BTSocketconnected = MainActivity.BTsocket.isConnected();
                    }
                    if (BTSocketconnected) {
                    } else {
                        try {
                            MainActivity.BTsocket.connect();
                        } catch (IOException e) {
                            e.printStackTrace();

                            Log.v(TAG, "FAIL: Could not connect to Bluetooth");
                            Toast.makeText(Einstellungen.this, "Please turn on your microscope.",
                                    Toast.LENGTH_SHORT).show();

                            bluetoothmessage.setTextColor(Color.RED);
                            bluetoothmessage.setText("Bluetooth Connection Failed!");
                            bluetoothmessage.setVisibility(View.VISIBLE);

                        } catch (NullPointerException e) {
                            e.printStackTrace();

                            Log.v(TAG, "FAIL: Bluetooth probably not enabled");
                            Toast.makeText(Einstellungen.this, "Please turn on your Bluetooth.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        try {
                            //Open OutputStream "Channel"
                            outputStream = MainActivity.BTsocket.getOutputStream();
                            Log.v(TAG, "OutputStream opened");
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.v(TAG, "FAIL: Couldn't get Output Stream!");
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }

                        try {

                            //Send String to Raspberry PI
                            outputStream.write("New Device connected!".getBytes());
                            Log.v(TAG, "Connection Successful!");

                            bluetoothmessage.setTextColor(Color.GREEN);
                            bluetoothmessage.setText("Bluetooth Connection Succeded!");
                            bluetoothmessage.setVisibility(View.VISIBLE);

                            //outputStream.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.v(TAG, "FAIL: Couldn't write in OutputStream!");
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Toast.makeText(Einstellungen.this,
                            "You don't have your Bluetooth enabled",
                            Toast.LENGTH_SHORT).show();
                    finishDialogNoBluetooth();
                    bluetoothmessage.setTextColor(Color.RED);
                    bluetoothmessage.setText("Bluetooth Connection Failed!");
                    bluetoothmessage.setVisibility(View.VISIBLE);
                }
            }
        });

        //Gets SSID and PSK from Network and transmitt it over Bluetooth
        wlan_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String SSID = SSIDedeittext.getText().toString();
                final String PSK = PSKedittext.getText().toString();
                final String WLANConnectionData = "" + SSID + "@" + PSK;
                if (MainActivity.BTsocket != null) {
                    try {
                        outputStream.write(WLANConnectionData.getBytes());
                        outputStream.flush();
                        for (int i = 0; i < 20; i++) {
                            outputStream.write("IP".getBytes());
                            outputStream.flush();
                            inputStream = MainActivity.BTsocket.getInputStream();
                            IPaddress = toStringInputStream(inputStream);
                            if (inputStream != null) {
                                break;
                            }
                        }
                        //Success + Print IP as proof
                        IP.setText("Your Microscopes IP: " + IPaddress);
                        IP.setTextColor(Color.GREEN);
                        IP.setVisibility(View.VISIBLE);
                    } catch (IOException e) {
                        //FAIL
                        e.printStackTrace();
                        IP.setText("Could not transmitt Network Connection Data. Check Bluetooth Connection");
                        IP.setTextColor(Color.RED);
                        IP.setVisibility(View.VISIBLE);
                    } catch (NullPointerException e) {
                        //FAIL
                        e.printStackTrace();
                        IP.setText("Could not transmitt Network Connection Data. Check Bluetooth Connection");
                        IP.setTextColor(Color.RED);
                        IP.setVisibility(View.VISIBLE);
                    }
                } else {
                    IP.setText("Could not transmitt Network Connection Data. Check Bluetooth Connection");
                    IP.setTextColor(Color.RED);
                    IP.setVisibility(View.VISIBLE);
                }

            }

        });
    }

    /**
     * Transforms Inputstream.read() into a String
     *
     * @param is InputStream
     * @return String
     * @throws IOException
     */
    private static String toStringInputStream(InputStream is) throws IOException {
        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (is, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            for (int i = 0; i < 15; i++) {
                c = reader.read();
                textBuilder.append((char) c);
            }
        }
        return textBuilder.toString();
    }
}




