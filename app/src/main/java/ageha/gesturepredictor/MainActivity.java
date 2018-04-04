package ageha.gesturepredictor;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class MainActivity extends WearableActivity implements SensorEventListener{

    private TextView mTextView;
    private Button startBtn;
    private Vibrator vibrator;
    private TimeStart timer;
    private boolean isRecording;
    private long sensorTimeReference = 0L;
    private long myTimeReference = 0L;
    private ArrayList<float[]> allData = new ArrayList<>();
    private ArrayList<Long> allTime = new ArrayList<>();
    private float[] preMag;
    private float[] preGravity;

    private static final int FRAME_NUMBER = 14;

    private Classifier classifier;
    private static final String MODEL_PATH = "foo.tflite";
    private Executor executor = Executors.newSingleThreadExecutor();
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
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Enables Always-on
        setAmbientEnabled();
        isRecording = false;

        initTensorFlowAndLoadModel();
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
                    isRecording = false;
                    setResult();
                }
            };
            countDownTimer.start();
        }
    }

    private void vibrate(){
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(150,VibrationEffect.DEFAULT_AMPLITUDE));
        }else{
            //deprecated in API 26
            vibrator.vibrate(150);
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
                    allTime.add(myTimeReference + Math.round((event.timestamp - sensorTimeReference) / 1000000.0));
                    allData.add(combine12Acc(getEarchAcc(preMag, event.values, preGravity)));

//                    System.out.println(Arrays.toString(event.values));
//                    System.out.println("earth:" + Arrays.toString(getEarchAcc(preMag, event.values, preGravity)));
                }
            }
        }
    }


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

    private float[] combine12Acc(float[] acc_123){
        return new float[]{acc_123[2],(float) Math.sqrt(Math.pow(acc_123[0],2) + Math.pow(acc_123[1],2))};
    }

//    static <T> T[] append(T[] arr, T element) {
//        final int N = arr.length;
//        arr = Arrays.copyOf(arr, N + 1);
//        arr[N] = element;
//        return arr;
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = MainModel.create(
                            getAssets(),
                            MODEL_PATH);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    private void setResult(){

        final String results = classifier.recognizeGesture(getFrameFeature());
        mTextView.setText("Result:" + results);
    }

    private float[][][] getFrameFeature(){
//        ratio = float(len(array)) / float(size+1)
//        res = []
//        for i in range(size):
//        res.append(np.mean(array[math.floor(i*ratio):math.ceil((i+2.0)*ratio)], axis = 0))
//        return np.array(res)
        float ratio = allData.size() / (FRAME_NUMBER + 1);
        float[][][] result = new float[1][14][2];
        int startIdx;
        int endIdx;
        for (int i = 0; i < FRAME_NUMBER; i++){
            startIdx = (int) Math.floor(i*ratio);
            endIdx = (int) Math.ceil((i+2.0)*ratio);
            result[0][i][0] = getMean(allData.subList(startIdx, endIdx),0);
            result[0][i][1] = getMean(allData.subList(startIdx, endIdx),1);
        }
        return result;

    }

    private float getMean(List<float[]> arr, int column){
        float sum = 0;
        for (float[] t : arr){
            sum+=t[column];
        }
        return sum/arr.size();
    }
}
