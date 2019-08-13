package com.seminarfach.smartlense;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.Set;

/**
 * Includes several Methods for Bluetooth Connection
 *
 * @author Marc Beling
 */
class BluetoothConnector {

    private boolean BTenabled = true;
    private final String TAG = "BluetoothConnector.java";

    /**
     * Constructor of BluetoothConnector class, Implemented in Main Class
     */
    BluetoothConnector() {
    }

    /**
     * Checks for devices' ability for Bluetooth.
     *
     * @return boolean: true, if Device has Bluetooth components
     */
    final boolean checkBT() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            //Display a toast notifying the user that their device doesn’t support Bluetooth//
            Log.v(TAG, "Bluetooth not supported");
            BTenabled = false;
        }
        return BTenabled;
    }

    /*public void onActivityResult(int mrequestCode, int mresultCode) {
        //Check what request we’re responding to//
        if (mrequestCode == ENABLE_BT_REQUEST_CODE) {
            //If the request was successful…//
            if (mresultCode == Activity.RESULT_OK) {
                //...then display the following toast.//
                Log.v(TAG, "Enabling Bluetooth successful");
            }
            //If the request was unsuccessful...//
            if (mresultCode == RESULT_CANCELED) {
                //...then display this alternative toast.//
                Log.v(TAG, "Enabling Bluetooth FAILED");
            }
        }
    }*/

    /**
     * Get all currently paired Bluetooth Devices
     *
     * @param mBluetoothAdapter   BluetoothAdapter: get it with getDefaultAdapter()
     * @param mArrayAdapter       ArrayAdapter: Part of ListView, can be displayed
     *                            if user has to choose between multiple Raspberry at the time
     * @param requestedMacAddress String: MAC-Address of certain Device,
     *                            that should be connected with
     * @return BluetoothDevice: Returns the instance of the certain BluetoothDevice
     * if not found returns <i>null</i>
     */
    BluetoothDevice getBTpaired(BluetoothAdapter mBluetoothAdapter,
                                ArrayAdapter mArrayAdapter,
                                String requestedMacAddress) {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there’s 1 or more paired devices...//
        if (pairedDevices.size() > 0) {
            //...then loop through these devices//
            for (BluetoothDevice device : pairedDevices) {
                //Retrieve each device’s public identifier and MAC address.
                //Add each device’s name and address to an ArrayAdapter, ready to incorporate into a
                //ListView
                //TODO: Unchecked call to 'add(T)' as a member of raw type 'android.widget.ArrayAdapter' less...
                // - Inspection info: Signals places where an unchecked warning is issued by the compiler, for example
                // - (IntellJ recommended)
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                if (device.getAddress().equals(requestedMacAddress)) {
                    Log.v(TAG, "Certain Device found: " + device.getAddress());
                    return device;
                } else {
                    Log.v(TAG, "Other Device found: " + device.getAddress());
                }
            }
        }
        return null;
    }

    /**
     * Sends a Discovery Request. Necessary for a Bluetooth Connection. No idea why.
     *
     * @param mbluetoothAdapter BluetoothAdapter: Get it with getDefaultAdapter()
     */
    void sendDiscoverRequest(BluetoothAdapter mbluetoothAdapter) {
        if (mbluetoothAdapter.startDiscovery()) {
            //If discovery has started, then display the following log....//
            Log.v(TAG, "Discovering other Bluetooth Devices");
        } else {
            //If discovery hasn’t started, then display this alternative Log//
            Log.v(TAG, "FAIL: No Discovery started yet.");
        }
    }
}



