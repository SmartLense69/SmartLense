package com.seminarfach.smartlense;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static com.seminarfach.smartlense.Colorpicker.blueseekvalue;
import static com.seminarfach.smartlense.Colorpicker.greenseekvalue;
import static com.seminarfach.smartlense.Colorpicker.redseekvalue;
import static com.seminarfach.smartlense.R.drawable;
import static com.seminarfach.smartlense.R.id;
import static com.seminarfach.smartlense.R.layout;

/**
 * Camera Activity Class. displays camera, takes pictures, includes
 * LED-Matrix control and button for color picker activity
 *
 * @author Marc Beling
 */
@SuppressWarnings("ALL")
public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    //TAG for logcat
    private static final String TAG = "CameraActivity";

    //Layout - elements
    Button capturebutton;
    Button colorpickerbutton;
    TableLayout lichttable;

    //Bluetooth - Transmit data to Raspberry Pi
    public OutputStream outputStream;

    //Camera
    public PackageManager pm;
    public Camera mCamera;

    //Get Camera Picture "mPicture"
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = CameraPreview.getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    /**
     * A safe way to get an instance of the CameraActivity object.
     *
     * @return Camera: Camera Instance
     **/
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a CameraActivity instance
        } catch (Exception e) {
            // CameraActivity is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //No Title should be displayed
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(layout.activity_camera);

        //Create Package Manager, relevant for Camera Instance
        Context contextActivity = this;
        pm = contextActivity.getPackageManager();

        //Create Camera
        mCamera = getCameraInstance();
        CameraPreview mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = findViewById(id.camera_preview);
        preview.addView(mPreview);

        //Capturebutton for Camera
        capturebutton = findViewById(id.capture);
        capturebutton.setOnClickListener(CameraActivity.this);

        //Colorpicker, leads to new activity ColorPicker
        colorpickerbutton = findViewById(id.colorpicker);
        colorpickerbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CameraActivity.this, Colorpicker.class);
                startActivity(intent);
            }
        });

        //FIXME: Change the background color of the square of the button.
        // - Currently, it stays at black although variables changed
        //get the color values of the Colorpicker Activity Class and sets the button background
        colorpickerbutton.setBackgroundColor(Color.rgb(redseekvalue, greenseekvalue, blueseekvalue));

        //TableLayout - LED Matrix 8*8
        LayoutInflater inflater = this.getLayoutInflater();
        final View lighttableView = inflater.inflate(layout.lighttable_preview, null);
        lichttable = lighttableView.findViewById(id.lightcontroll);
        lichttable.setPadding(1, 0, 0, 1);

        int id = 0;
        for (int f = 0; f <= 7; f++) {
            //Create the rows
            TableRow tableRow = new TableRow(this);
            for (int c = 0; c <= 7; c++) {
                id++;
                final Button b = new Button(this);
                b.setBackgroundResource(drawable.button_unimpressed);
                tableRow.addView(b, 100, 100);
                final int finalid = id;
                b.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {

                        String finalColorValue = Colorpicker.getColorvalue();
                        String colorcommand = (finalid + finalColorValue);

                        //Checks if Bluetooth or WLAN is chosen in the options
                        //Sets Button to the Color chosen.
                        if (Einstellungen.wifioderbluetooth) { //if Bluetooth
                            try {
                                outputStream = MainActivity.BTsocket.getOutputStream();
                                outputStream.write(colorcommand.getBytes());
                                //Flushing and closing the outputStream is necessary for separated
                                //Strings
                                outputStream.flush();
                                outputStream.close();
                                b.setBackgroundColor(Color
                                        .rgb(redseekvalue, greenseekvalue, blueseekvalue));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else { //if WLAN
                            new WLANconnection().execute("http://192.168.178.59:5000/" + colorcommand);
                            b.setBackgroundColor(Color.rgb(redseekvalue, greenseekvalue, blueseekvalue));
                        }
                    }
                });
            }
            //Displays table
            lichttable.addView(tableRow);
        }

        //ToggleButton LED Matrix
        //1st initialisation (to prevent Nullpointerexception)
        super.addContentView(lighttableView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        lighttableView.setVisibility(View.INVISIBLE);
        ToggleButton toggleMatrix = findViewById(R.id.matrixtogglebutton);
        toggleMatrix.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) { //Toggle LED Matrix
                    lighttableView.setVisibility(View.VISIBLE);
                } else {
                    lighttableView.setVisibility(View.INVISIBLE);
                }
            }
        });

    }

    @Override
    public void onClick(View v) {
        //Takes Picture
        //FIXME: On Real Device: When trying to capture, picture freezes,
        // - when trying to take another one, RunTimeError: taking picture failed.
        // - On Emulator: picture doesn't show up, NullPointerException when pressing Capture.
        mCamera.takePicture(null, null, mPicture);
    }

}

