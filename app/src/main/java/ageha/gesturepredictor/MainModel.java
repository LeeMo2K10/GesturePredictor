package ageha.gesturepredictor;

import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainModel implements Classifier{


    private Interpreter interpreter;
    private List<String> labelList;

    private MainModel() {

    }

    static Classifier create(AssetManager assetManager,
                             String modelPath) throws IOException {

        MainModel classifier = new MainModel();
        classifier.interpreter = new Interpreter(classifier.loadModelFile(assetManager, modelPath));
        classifier.labelList = classifier.loadLabelList();

        return classifier;
    }


    public String recognizeGesture(float[][][] f) {
        float[][] result = new float[1][labelList.size()];
        System.out.println(Arrays.toString(f));
        interpreter.run(f, result);
        System.out.println("Result:" + Arrays.toString(result[0]));
        return getNamedResult(result[0]);
    }


    @Override
    public void close() {
        interpreter.close();
        interpreter = null;
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabelList(){
        List<String> labelList = new ArrayList<>();

        String s;
        for (int i = 0; i<10; i++){
            s = "Tag" + i;
            labelList.add(s);
        }
        return labelList;
    }




    @SuppressLint("DefaultLocale")
    private String getNamedResult(float[] labelProbArray) {

        float max_prob = 0;
        int max_idx = -1;
        for (int idx = 0; idx<labelProbArray.length;idx++){
            if (labelProbArray[idx] > max_prob){
                max_idx = idx;
                max_prob = labelProbArray[idx];
            }
        }

        return labelList.get(max_idx);
    }

}
