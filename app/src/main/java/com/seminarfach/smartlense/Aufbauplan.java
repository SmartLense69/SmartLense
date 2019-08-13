package com.seminarfach.smartlense;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

/**
 * Displays a tutorial on how to build the microscope
 *
 * @author Marc Beling
 */
public class Aufbauplan extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aufbauplan);
        TextView yourTextView = findViewById(R.id.textView2);
        yourTextView.setMovementMethod(new ScrollingMovementMethod());
    }
}
