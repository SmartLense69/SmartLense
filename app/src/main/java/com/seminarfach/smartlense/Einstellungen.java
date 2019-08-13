package com.seminarfach.smartlense;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * Choosing between WLAN and Bluetooth and getting several network address, given by user
 *
 * @author Marc Beling
 */
@SuppressWarnings("ALL")
public class Einstellungen extends AppCompatActivity {

    //Tag for logcat
    public final String TAG = "Einstellungen.java";

    //Bluetooth Stuff
    //BT Socket is required for Camera Activity
    public static Boolean wifioderbluetooth;
    public OutputStream outputStream;
    public String BTAdress = "B8:27:EB:E6:8A:FD";
    public boolean mEnablingBT = false;
    public boolean BTSocketconnected;
    public boolean failure = false;

    //startActivityforResult has to be in a method block

    /**
     * startActivityforResult, but in a method block
     *
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
     *
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
     *
     * @param mItemsPairedDevices Array Adapter: List of paired devices
     * @param mBTAddress          String: MAC Address of the certain device
     * @return Bluetooth Socket: Later used for connection
     */
    public BluetoothSocket finalBTConnect(ArrayAdapter mItemsPairedDevices, String mBTAddress) {

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

        //Get Bluetooth Device
        BluetoothDevice BTdevice =
                MainActivity.bluetoothConnector.
                        getBTpaired(mBTAdapter, mItemsPairedDevices, mBTAddress);
        if (BTdevice == null) {
            Log.v(TAG, "Raspberry Pi not found!");
            Toast.makeText(this, "Your Raspberry wasn't found.", Toast.LENGTH_SHORT).show();
            return null;
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
        Button bluetooth_connect = findViewById(R.id.bluetooth_connect);

        //Create ListView for paired Devices.
        // Not neccessary to display, but required for Method finalBTConnect
        final ArrayAdapter<String> itemsPairedDevices =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        ListView listView = findViewById(R.id.pairedDevices);
        listView.setAdapter(itemsPairedDevices);
        listView.setVisibility(View.INVISIBLE);

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
                        MainActivity.BTsocket = finalBTConnect(itemsPairedDevices, BTAdress);
                    }
                    if (MainActivity.BTsocket == null) {
                        Log.v(TAG, "FAIL: Bluetooth probably not enabled");

                    } else {
                        BTSocketconnected = MainActivity.BTsocket.isConnected();
                    }
                    if (BTSocketconnected && MainActivity.BTsocket == null) {
                    } else {
                        try {
                            MainActivity.BTsocket.connect();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.v(TAG, "FAIL: Could not connect to Bluetooth");
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                            Log.v(TAG, "FAIL: Bluetooth probably not enabled");
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
                }
            }
        });
    }
}




