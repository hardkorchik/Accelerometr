package anna.accelerometer17_01_16v2;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;


public class MainActivity<N> extends Activity implements SensorEventListener {
    static SharedPreferences settings;
    static SharedPreferences.Editor editor;

    private int SENSORPERIOD = 10;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float lastX = 0;
    private float lastY = 0;
    private float lastZ = 0;

    private File lastFile;

    int N = 1500;
    private float arrX[] = new float[N];
    private float arrY[] = new float[N];
    private float arrZ[] = new float[N];
    private double time[] = new double[N];
    int i = 0;
    private long curent_time;

    private TextView txtCurrentX, txtCurrentY, txtCurrentZ, txtDeltaX, txtDeltaY, txtDeltaZ;//, maxX, maxY, maxZ, txtOutput;
    private EditText txtFilename;// txtPeriod,
    private Button btnStart;
    private Button btnSendEmail;
    private Drawable btnStartBackground;
    boolean recording = false;
    boolean indicatorBlink = false;

    PorterDuffColorFilter filter;

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeElements();
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        editor = settings.edit();
        filter = new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert sensorManager != null;
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
            final Handler mHandler = new Handler();
            final Runnable mAction = new Runnable() {

                @Override

                public void run() {
                    arrX[i] = lastX;
                    arrY[i] = lastY;
                    arrZ[i] = lastZ;
                    time[i] = System.nanoTime();
                    i++;
                    if (i < N) { mHandler.postDelayed(this, SENSORPERIOD); }
                    else {
                        btnStart.setText("Начать запись");
                        btnStart.setEnabled(true);
                    }
                }
            };

            btnStart.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!recording) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        recording = true;
                        SENSORPERIOD = 10;
                        btnStart.setText("Остановить запись");
                        //curent_time = System.nanoTime();
                        mHandler.post(mAction);
                    } else {
                        recording = false;
                    }
                }
            });


            btnSendEmail.setOnClickListener(new OnClickListener() {
                /*@Override
                public void onClick(View view) {

                    int SDK_INT = android.os.Build.VERSION.SDK_INT;
                    if (SDK_INT > 8) {
                        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                                .permitAll().build();
                        StrictMode.setThreadPolicy(policy);

                    }
                    String postReceiverUrl = "http://tomchuk.ru/accel/";

                    sendPost(postReceiverUrl, data, txtFilename.getText().toString());
                }*/
                @Override
                public void onClick(View view) {
                    lastFile = new File(settings.getString("lastFileName", "0"));
                    if (lastFile.exists()) {
                        Intent email = new Intent(Intent.ACTION_SEND);
                        email.putExtra(Intent.EXTRA_EMAIL, new String[]{"Anna.T.Accelerometer@gmail.com"});
                        email.putExtra(Intent.EXTRA_SUBJECT, "Данные с акселерометра");
                        email.putExtra(Intent.EXTRA_TEXT, "");
                        Uri uri = Uri.parse("file://" + lastFile.getAbsolutePath());
                        email.putExtra(Intent.EXTRA_STREAM, uri);
                        email.setType("message/rfc822");
                        startActivity(Intent.createChooser(email, "Choose an Email App:"));
                    } else {
                        Toast.makeText(MainActivity.this, "Последний файл не был найден " + lastFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    }


                }
            });
        }
    }


    public static final String USER_AGENT = "Mozilla/5.0";

    public static String sendPost(String _url, String data, String txtFilename) {

        String result = "";
        try {
            URL obj = new URL(_url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Accept-Language", "UTF-8");

            con.setDoOutput(true);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(con.getOutputStream());
            outputStreamWriter.write(txtFilename + "\n");
            outputStreamWriter.write(data);
            outputStreamWriter.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine).append("\n");
            }
            in.close();

            result = response.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return result;
        }

    }

    public void playClick(int index) {
        MediaPlayer mediaPlayer;
        if (index == 1) {
            mediaPlayer = MediaPlayer.create(this, R.raw.begin);

            mediaPlayer.start();
        }
        if (index == 2) {
            mediaPlayer = MediaPlayer.create(this, R.raw.average);

            mediaPlayer.start();
        }
        if (index == 3) {
            mediaPlayer = MediaPlayer.create(this, R.raw.end);
            //System.out.print("Hellow");
            mediaPlayer.start();
        }
    }

    private void indicatorBlink() {
        if (indicatorBlink) {
            indicatorBlink = false;
            btnStart.invalidateDrawable(btnStartBackground);
            btnStartBackground.clearColorFilter();
        } else {
            indicatorBlink = true;
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
        btnStart = (Button) findViewById(R.id.btnStartRecord);
        btnStartBackground = btnStart.getBackground();
        btnSendEmail = (Button) findViewById(R.id.btnSendEmail);
        txtFilename = (EditText) findViewById(R.id.txtFilename);
    }

    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
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

        lastX = event.values[0];
        lastY = event.values[1];
        lastZ = event.values[2];
    }

    public void displayCleanValues() {
        txtCurrentX.setText("0.0");
        txtCurrentY.setText("0.0");
        txtCurrentZ.setText("0.0");
    }

    @SuppressLint("SetTextI18n")
    public void displayCurrentValues() {
        //txtCurrentX.setText(Float.toString(lastX));
        //txtCurrentY.setText(Float.toString(lastY));
        //txtCurrentZ.setText(Float.toString(lastZ));
        //txtDeltaX.setText(Float.toString(deltaX));
        //txtDeltaY.setText(Float.toString(deltaY));
        //txtDeltaZ.setText(Float.toString(deltaZ));
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private StringBuilder createSB() {
        StringBuilder sb = new StringBuilder();

        int i = 0;
        curent_time = (long) time[0];
        while (i < N - 1) {
            sb.append((time[i] - curent_time) / 1000).append(" ").append(arrX[i]).append(" ").append(arrY[i]).append(" ").append(arrZ[i]).append(" ").append('\n');
            i++;
        }
        writeToSDCard(sb.toString());

        return sb;
    }

    private void writeToSDCard(String data) {
        File extDir = getExternalFilesDir(null);
        assert extDir != null;
        String filename;
        filename = txtFilename.getText() + ".txt";
        File file = new File(extDir, filename);
        if (checkExternalStorage()) {
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(file);
                fos.write(data.getBytes());
                fos.close();
                editor.putString("lastFileName", file.getAbsolutePath());
                editor.commit();
                Toast.makeText(this, "Создан файл " + filename, Toast.LENGTH_LONG).show();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean checkExternalStorage() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }
}