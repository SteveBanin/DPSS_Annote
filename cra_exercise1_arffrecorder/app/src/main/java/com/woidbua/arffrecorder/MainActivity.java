package com.woidbua.arffrecorder;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    /**
     * Fields
     */
    // view components
    private TextView mAccValuesTextView;
    private EditText mFilenameValueEditText;
    private ToggleButton mRecordToggleButton;

    // sensors
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    // shared preferences
    SharedPreferences mSharedPrefs;
    private static final String PREF_FILENAME = "PREF_FILENAME";
    private static final String PREF_RECORDER_STATE = "PREF_RECORDER_STATE";

    /**
     * Methods - starting process
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.initSensorComponents();
        this.initViewComponents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.loadSharedPreferences();
    }

    private void initSensorComponents() {
        this.mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        this.mAccelerometer = this.mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void initViewComponents() {
        this.mAccValuesTextView = findViewById(R.id.accelerometerValuesTextView);
        this.mFilenameValueEditText = findViewById(R.id.filenameValueEditText);
        this.mRecordToggleButton = findViewById(R.id.recordToggleButton);
        this.mRecordToggleButton.setOnClickListener(this.startStopRecording());
    }

    private View.OnClickListener startStopRecording() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEmptyFilename()) {
                    MainActivity.this.mRecordToggleButton.setChecked(false);
                    Toast.makeText(MainActivity.this,
                            "Please insert a filename!", Toast.LENGTH_LONG).show();
                    return;
                }

                if (MainActivity.this.mRecordToggleButton.isChecked()) {
                    MainActivity.this.mSensorManager.registerListener(MainActivity.this, MainActivity.this.mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
                    MainActivity.this.mFilenameValueEditText.setEnabled(false);
                } else {
                    MainActivity.this.mSensorManager.unregisterListener(MainActivity.this);
                    MainActivity.this.mFilenameValueEditText.setEnabled(true);
                    MainActivity.this.mAccValuesTextView.setText("X: , Y: , Z: ");
                }
            }
        };
    }

    private boolean isEmptyFilename() {
        return this.mFilenameValueEditText.getText().toString().trim().equals("");
    }

    private void loadSharedPreferences() {
        this.mSharedPrefs = getPreferences(Context.MODE_PRIVATE);
        this.mFilenameValueEditText.setText(mSharedPrefs.getString(PREF_FILENAME, ""));
        this.mRecordToggleButton.setChecked(mSharedPrefs.getBoolean(PREF_RECORDER_STATE, false));
    }

    /**
     * Methods - ending process
     */
    @Override
    protected void onPause() {
        super.onPause();
        this.saveSharedPreferences();
    }

    /**
     * Methods - sensor events
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        updateAccValues(event.values);
        this.updateView(event.values);
    }

    /**
     * Methods - file writing
     */
    private void updateAccValues(float[] values) {
        long timestamp = System.currentTimeMillis();
        String filePath = this.mFilenameValueEditText.getText().toString();

        if (!fileExists(filePath)) {
            try {
                this.insertText(filePath, this.getFileHeader());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            this.insertText(filePath, this.getFileDataFormat(timestamp, values));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean fileExists(String filepath) {
        File file = getBaseContext().getFileStreamPath(filepath);
        return file.exists();
    }

    private void insertText(String filepath, String content) throws IOException {
        content += "\n"; // for line break
        OutputStream outputStream = openFileOutput(filepath, Context.MODE_APPEND);
        outputStream.write(content.getBytes());
        outputStream.close();
    }

    private String getFileHeader() {
        String header = "";

        // comment block
        header += "% Group: LeisterMeierMulterer\n";
        header += "% Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.GERMANY)
                .format(new Date()) + "\n\n";

        // relation
        header += "@RELATION sysdev\n\n";

        // attributes
        header += "@ATTRIBUTE timestamp     NUMERIC\n";
        header += "@ATTRIBUTE accelerationx NUMERIC\n";
        header += "@ATTRIBUTE accelerationy NUMERIC\n";
        header += "@ATTRIBUTE accelerationz NUMERIC\n\n";

        // data segment
        header += "@Data"; // last line break added in write method

        return header;
    }

    private String getFileDataFormat(long timestamp, float[] values) {
        return String.format(Locale.ENGLISH,
                "%d,%.1f,%.1f,%.1f",
                timestamp, values[0], values[1], values[2]);
    }

    /**
     * Methods - general
     */
    private void saveSharedPreferences() {
        SharedPreferences.Editor editor = this.mSharedPrefs.edit();
        editor.putString(PREF_FILENAME, this.mFilenameValueEditText.getText().toString());
        editor.putBoolean(PREF_RECORDER_STATE, this.mRecordToggleButton.isChecked());
        editor.apply();
    }


    private void updateView(float[] values) {
        this.mAccValuesTextView.setText(String.format(Locale.GERMANY,
                "X: %.1f, Y: %.1f, Z: %.1f",
                values[0], values[1], values[2]));
    }
}
