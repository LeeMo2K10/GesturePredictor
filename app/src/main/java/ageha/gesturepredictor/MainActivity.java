package ageha.gesturepredictor;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.util.Arrays;


public class MainActivity extends WearableActivity implements SensorEventListener{

    private TextView mTextView;
    private Button startBtn;
    private Vibrator v;
    private TimeStart timer;
    private boolean isRecording;
    private long sensorTimeReference = 0L;
    private long myTimeReference = 0L;
    private FileOutputStream fos = null;
    private StringBuilder sb = new StringBuilder();
    private float[] preMag;
    private float[] preGravity;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.text);
        startBtn = findViewById(R.id.startBtn);
        startBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
//                createFile();
                startBtn.setVisibility(View.INVISIBLE);
                mTextView.setVisibility(View.VISIBLE);
                timer.run();
            }
        });

        RegisterSensors();
        timer = new TimeStart();
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Enables Always-on
        setAmbientEnabled();
        isRecording = false;
    }

    private class TimeStart extends Thread{
        public void run(){
            Log.i("main_activity", "TimeStart");
            CountDownTimer countDownTimer = new CountDownTimer( 6 * 1000, 1000){
                String s;
                @Override
                public void onTick(long millisUntilFinish){
                    int secondsLeft = (int)millisUntilFinish/1000;

                    if (secondsLeft > 2){
                        s = "Ready " + (secondsLeft - 2);
                        mTextView.setText(s);
                        vibrate();
                    }
                    else{
                        isRecording = true;
                        s = "Recording";
                        mTextView.setText(s);
                    }

                }
                @Override
                public void onFinish(){
                    s = "Result: 2";
                    mTextView.setText(s);
                    isRecording = false;
                }
            };
            countDownTimer.start();
        }
    }

    private void vibrate(){
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(150,VibrationEffect.DEFAULT_AMPLITUDE));
        }else{
            //deprecated in API 26
            v.vibrate(150);
        }
    }

    private void RegisterSensors(){
        int sampling_rate = SensorManager.SENSOR_DELAY_FASTEST;
        //        Register Sensors
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        assert sensorManager != null;
        for(Sensor i:sensorManager.getSensorList(Sensor.TYPE_ALL)){
            sensorManager.registerListener(this, i, sampling_rate);

        }
//        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), sampling_rate);
//        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), sampling_rate);
//        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), sampling_rate);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isRecording) {
            // calculate timestamp
            if (sensorTimeReference == 0L && myTimeReference == 0L) {
                sensorTimeReference = event.timestamp;
                myTimeReference = System.currentTimeMillis();
            }
            if (event.sensor.getType() == Sensor.TYPE_GRAVITY){
                preGravity = event.values;
            }
            else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
                preMag = event.values;
            }
            else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
                if ((preMag != null) & (preGravity != null)){
//                    WriteSensorEvent(myTimeReference +
//                                    Math.round((event.timestamp - sensorTimeReference) / 1000000.0),
//                            getEarchAcc(preMag, event.values, preGravity));
                    System.out.println(Arrays.toString(event.values));
                    System.out.println("earth:" + Arrays.toString(getEarchAcc(preMag, event.values, preGravity)));
                }
            }
        }
    }



    private void WriteSensorEvent(long time,float[] values){
//        try {

            sb.append(String.valueOf(time));
            for (float i:values){
                sb.append(", ").append(String.valueOf(i));
            }
            sb.append('\n');
//            if (sb.length() > 2500){
//                fos.write(sb.toString().getBytes());
//                sb.setLength(0);
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

//    private void createFile(){
//            if (fos != null){
//                return;
//            }
//            String file_path = getDocStorageDir(getBaseContext()).getAbsolutePath() + "/" + "temp.csv";
//            try {
//                fos = new FileOutputStream(new File(file_path));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            try{
//                fos.write("TIMESTAMP,VALUES1,VALUES2,VALUES3 \n".getBytes());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//    private File getDocStorageDir(Context context) {
//        return new File(context.getExternalFilesDir(
//                Environment.DIRECTORY_DOCUMENTS), "DATA");
//    }

    private float[] getEarchAcc(float[] mag, float[] acc, float[] gravity){
        if (acc.length == 3){
            acc = new float[] {acc[0],acc[1],acc[2],0.0f};
        }
        float[] R = new float[16], I = new float[16], earthAcc = new float[16];

        SensorManager.getRotationMatrix(R, I, gravity, mag);

        float[] inv = new float[16];
//
        android.opengl.Matrix.invertM(inv, 0, R, 0);
        android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, acc, 0);

        return new float[]{earthAcc[0],earthAcc[1],earthAcc[2]};
    }

    static <T> T[] append(T[] arr, T element) {
        final int N = arr.length;
        arr = Arrays.copyOf(arr, N + 1);
        arr[N] = element;
        return arr;
    }
}
