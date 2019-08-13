package com.seminarfach.smartlense;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * MainActivity. Constructs Bluetooth Connector, so it will be always loaded.
 *
 * @author Marc Beling
 */
public class MainActivity extends AppCompatActivity {

    static BluetoothConnector bluetoothConnector = new BluetoothConnector();
    static BluetoothSocket BTsocket = null;

    Button GoToAufbauplan;
    Button GoToKamera;
    Button GoToSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Button für Aufbauplan
        setContentView(R.layout.activity_main);
        GoToAufbauplan = findViewById(R.id.button_aufbauplan);
        GoToAufbauplan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Aufbauplan.class);
                startActivity(intent);

            }
        });

        //Button für Kamera
        GoToKamera = findViewById(R.id.button_camera);
        GoToKamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);

            }
        });

        //Button für Einstellungen
        GoToSettings = findViewById(R.id.button_settings);
        GoToSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Einstellungen.class);
                startActivity(intent);

            }
        });

    }
}

