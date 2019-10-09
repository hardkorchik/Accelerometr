package anna.accelerometer17_01_16v2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.View.OnClickListener;
import android.view.View;
import android.os.Bundle;
import android.os.Handler;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.text.format.Time;
import android.text.method.ScrollingMovementMethod;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.LinkedList;
import java.util.ListIterator;


public class MainActivity extends Activity implements SensorEventListener {
    static SharedPreferences settings;
    static SharedPreferences.Editor editor;

    final String FILENAME = "am_log";
    private int SENSORPERIOD = 10;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float lastX = 0;
    private float lastY = 0;
    private float lastZ = 0;

    private File lastFile;

    /*private float deltaXMax = 0;
    private float deltaYMax = 0;
    private float deltaZMax = 0;*/

    private float deltaX = 0;
    private float deltaY = 0;
    private float deltaZ = 0;

    private LinkedList<Float> arrX = new LinkedList<>();;
    private LinkedList<Float> arrY = new LinkedList<>();;
    private LinkedList<Float> arrZ = new LinkedList<>();;

    private StringBuilder sb = new StringBuilder();

    private TextView txtCurrentX, txtCurrentY, txtCurrentZ, txtDeltaX, txtDeltaY, txtDeltaZ, maxX, maxY, maxZ, txtOutput;
    private EditText txtPeriod;
    private Button btnStart;
    private Button btnSendEmail;
    private Drawable btnStartBackground;
    boolean recording = false;
    boolean indicatorBlink = false;
    PorterDuffColorFilter filter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeElements();
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        editor = settings.edit();
        filter = new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

            final Handler mHandler = new Handler();
            final Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    arrX.add(lastX);
                    arrY.add(lastY);
                    arrZ.add(lastZ);

                    if(recording == true) {
                        indicatorBlink();
                        mHandler.postDelayed(this, SENSORPERIOD);
                    }else{
                        indicatorBlink = true;
                        indicatorBlink();
                        btnStart.setText("Начать запись");
                        txtPeriod.setEnabled(true);
                        StringBuilder sb = createSB();
                        txtOutput.setText(sb.toString());
                        writeToSDCard(sb.toString());
                    }
                }
            };

            btnStart.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (recording == false) {
                        recording = true;
                        SENSORPERIOD = Integer.parseInt(txtPeriod.getText().toString());
                        btnStart.setText("Остановить запись");
                        txtPeriod.setEnabled(false);
                        arrX.clear();
                        arrY.clear();
                        arrZ.clear();
                        mHandler.post(mAction);
                    }else{
                        recording = false;
                    }
                }
            });

            btnSendEmail.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    lastFile = new File(settings.getString("lastFileName","0"));
                    if(lastFile.exists())
                    {
                        Intent email = new Intent(Intent.ACTION_SEND);
                        email.putExtra(Intent.EXTRA_EMAIL, new String[]{"Anna.T.Accelerometer@gmail.com"});
                        email.putExtra(Intent.EXTRA_SUBJECT, "Данные с акселерометра");
                        email.putExtra(Intent.EXTRA_TEXT, "");
                        Uri uri = Uri.parse("file://" + lastFile.getAbsolutePath());
                        email.putExtra(Intent.EXTRA_STREAM, uri);
                        email.setType("message/rfc822");
                        startActivity(Intent.createChooser(email, "Choose an Email App:"));
                    }
                    else
                    {
                        Toast.makeText(MainActivity.this, "Последний файл не был найден " + lastFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    }


                }
            });
            /*btnStart.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    if(recording == false){
                        recording = true;
                        btnStart.setText("Остановить запись");
                        arrX.clear();
                        arrY.clear();
                        arrZ.clear();
                    }
                    else{
                        recording = false;
                        btnStart.setText("Начать запись");
                        StringBuilder sb = createSB();
                        //txtOutput.setText(sb.toString()); //TODO debug textview here
                        writeToSDCard(sb.toString());
                    }
                }
            });*/
        }
    }

    private void indicatorBlink(){
        if(indicatorBlink){
            indicatorBlink = false;
            //btnStart.setBackgroundResource(android.R.drawable.btn_default);
            btnStart.invalidateDrawable(btnStartBackground);
            btnStartBackground.clearColorFilter();
        }else{
            indicatorBlink = true;
            //btnStart.setBackgroundColor(Color.RED);
            btnStartBackground.setColorFilter(filter);
        }
    }

    public void initializeElements() {
        txtCurrentX = (TextView) findViewById(R.id.txtCurrentX);
        txtCurrentY = (TextView) findViewById(R.id.txtCurrentY);
        txtCurrentZ = (TextView) findViewById(R.id.txtCurrentZ);
        txtDeltaX = (TextView) findViewById(R.id.txtDeltaX);
        txtDeltaY = (TextView) findViewById(R.id.txtDeltaY);
        txtDeltaZ = (TextView) findViewById(R.id.txtDeltaZ);
        maxX = (TextView) findViewById(R.id.maxX);
        maxY = (TextView) findViewById(R.id.maxY);
        maxZ = (TextView) findViewById(R.id.maxZ);
        btnStart = (Button) findViewById(R.id.btnStartRecord);
        btnStartBackground = btnStart.getBackground();
        btnSendEmail = (Button) findViewById(R.id.btnSendEmail);
        txtPeriod = (EditText) findViewById(R.id.txtPeriod);
        txtOutput = (TextView) findViewById(R.id.txtOutput);
        txtOutput.setMovementMethod(new ScrollingMovementMethod());
    }

    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        displayCleanValues();
        displayCurrentValues();
        //displayMaxValues();
        deltaX = Math.abs(lastX - event.values[0]);
        deltaY = Math.abs(lastY - event.values[1]);
        deltaZ = Math.abs(lastZ - event.values[2]);

        if (deltaX < 1)
            deltaX = 0;
        if (deltaY < 1)
            deltaY = 0;
        if (deltaZ < 1)
            deltaZ = 0;

        lastX = event.values[0];
        lastY = event.values[1];
        lastY = event.values[1];
        lastZ = event.values[2];
        /*if(recording == true){
            arrX.add(lastX);
            arrY.add(lastY);
            arrZ.add(lastZ);
        }*/
    }

    public void displayCleanValues() {
        txtCurrentX.setText("0.0");
        txtCurrentY.setText("0.0");
        txtCurrentZ.setText("0.0");
    }

    public void displayCurrentValues() {
        /*txtCurrentX.setText(Float.toString(deltaX));
        txtCurrentY.setText(Float.toString(deltaY));
        txtCurrentZ.setText(Float.toString(deltaZ));*/
        txtCurrentX.setText(Float.toString(lastX));
        txtCurrentY.setText(Float.toString(lastY));
        txtCurrentZ.setText(Float.toString(lastZ));
        txtDeltaX.setText(Float.toString(deltaX));
        txtDeltaY.setText(Float.toString(deltaY));
        txtDeltaZ.setText(Float.toString(deltaZ));
    }

    /*public void displayMaxValues() {
        if (deltaX > deltaXMax) {
            deltaXMax = deltaX;
            maxX.setText(Float.toString(deltaXMax));
        }
        if (deltaY > deltaYMax) {
            deltaYMax = deltaY;
            maxY.setText(Float.toString(deltaYMax));
        }
        if (deltaZ > deltaZMax) {
            deltaZMax = deltaZ;
            maxZ.setText(Float.toString(deltaZMax));
        }
    }*/

    private StringBuilder createSB(){
        StringBuilder sb = new StringBuilder();
        ListIterator it_X = arrX.listIterator();
        ListIterator it_Y = arrY.listIterator();
        ListIterator it_Z = arrZ.listIterator();
        while(it_X.hasNext()) {
            sb.append(it_X.next()).append("\t").append(it_Y.next()).append("\t").append(it_Z.next()).append("\n");
        }
        return sb;
    }

    private void writeToSDCard(String data) {
        File extDir = getExternalFilesDir(null);
        String path = extDir.getAbsolutePath();
        String filename;
        Time currentTime = new Time(Time.getCurrentTimezone());
        currentTime.setToNow();
        filename = FILENAME + "_" + currentTime.monthDay + "_" + currentTime.month+1 + "_" + currentTime.year + "_" + currentTime.format("%k_%M_%S") + ".txt";
        //txtOutput.setText(filename);
        File file = new File(extDir, filename);
        if (checkExternalStorage()) {
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(file);
                fos.write(data.getBytes());
                fos.close();
                //lastFile = file;
                editor.putString("lastFileName", file.getAbsolutePath());
                editor.commit();
                Toast.makeText(this, "Создан файл " + filename, Toast.LENGTH_LONG).show();
                //Toast.makeText(this, "Создан файл " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public boolean checkExternalStorage() {
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        }
        return false;
    }
}