package ageha.gesturepredictor;

import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;


public class MainModel implements Classifier{


    private static final float THRESHOLD = 0.1f;
    private static final int MAX_RESULTS = 1;
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


    public List<Recognition> recognizeGesture(float[] f) {
        byte[][] result = new byte[1][labelList.size()];
        interpreter.run(convertFloatArrayToByteBuffer(f), result);
        return getSortedResult(result);
    }

    private ByteBuffer convertFloatArrayToByteBuffer(float[] f){
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 * 14 * 2);
        byteBuffer.order(ByteOrder.nativeOrder());
        
        return byteBuffer;
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
    private List<Recognition> getSortedResult(byte[][] labelProbArray) {

        PriorityQueue<Recognition> pq =
                new PriorityQueue<>(
                        MAX_RESULTS,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(Recognition lhs, Recognition rhs) {
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });

        for (int i = 0; i < labelList.size(); ++i) {
            float confidence = (labelProbArray[0][i] & 0xff) / 255.0f;
            if (confidence > THRESHOLD) {
                pq.add(new Recognition("" + i,
                        labelList.size() > i ? labelList.get(i) : "unknown",
                        confidence));
            }
        }

        final ArrayList<Recognition> recognitions = new ArrayList<>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }

        return recognitions;
    }

}
