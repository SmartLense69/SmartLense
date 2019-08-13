package com.seminarfach.smartlense;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Class for picking the color. Transmits the values to the camera activity.
 *
 * @author Marc Beling
 */
public class Colorpicker extends AppCompatActivity {

    public static int redseekvalue = 0;
    public static int greenseekvalue = 0;
    public static int blueseekvalue = 0;

    public String redseekHex = "00";
    public String greenseekHex = "00";
    public String blueseekHex = "00";
    public static String colorvalue = "000000";

    public TextView colorDisplay;
    public Button Backbutton;

    public static String getColorvalue() {
        return colorvalue;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_colorpicker);
        colorDisplay = findViewById(R.id.CD);
        final SeekBar redseek = findViewById(R.id.redseek);
        final SeekBar greenseek = findViewById(R.id.greenseek);
        final SeekBar blueseek = findViewById(R.id.blueseek);

        Backbutton = findViewById(R.id.backbutton);
        Backbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Colorpicker.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        redseek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                redseekvalue = redseek.getProgress();
                redseekHex = Integer.toHexString(redseekvalue);
                colorvalue = (redseekHex + greenseekHex + blueseekHex);
                colorDisplay.setBackgroundColor(Color.rgb(redseekvalue, greenseekvalue, blueseekvalue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        greenseek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                greenseekvalue = greenseek.getProgress();
                greenseekHex = Integer.toHexString(greenseekvalue);
                colorvalue = (redseekHex + greenseekHex + blueseekHex);
                colorDisplay.setBackgroundColor(Color.rgb(redseekvalue, greenseekvalue, blueseekvalue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        blueseek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                blueseekvalue = blueseek.getProgress();
                blueseekHex = Integer.toHexString(blueseekvalue);
                colorvalue = (redseekHex + greenseekHex + blueseekHex);
                colorDisplay.setBackgroundColor(Color.rgb(redseekvalue, greenseekvalue, blueseekvalue));


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


    }
}


