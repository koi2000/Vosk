package org.vosk.demo.Utils;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;

import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechStreamService;
import org.vosk.demo.entity.partialResult;
import org.vosk.demo.entity.results;

import java.util.ArrayList;
import java.util.List;

public class RecognitionListenerImpl implements RecognitionListener {

    private static final String TAG = "RecognitionListenerImpl";

    private StringBuilder sentence_read = new StringBuilder(); //需手动更新

    private List<partialResult> partialResults;
    private List<Double>confs;//需手动更新
    private List<String>words;//需手动更新
    private SpeechStreamService speechStreamService;
    private Handler handler;
    private List<Pair<String,Double>>wordsConf;
    //private double fluency = 0.01;

    public RecognitionListenerImpl(SpeechStreamService speech, Handler obj) {
        this.speechStreamService=speech;
        this.handler=obj;
        words = new ArrayList<>();
        confs = new ArrayList<>();
        wordsConf = new ArrayList<>();
    }

    public List<Double> getConfs() {
        return confs;
    }

    public List<String> getWords() {
        return words;
    }

    public StringBuilder getSentence_read() {
        return sentence_read;
    }

    public List<Pair<String, Double>> getWordsConf() {
        return wordsConf;
    }

    @Override
    public void onPartialResult(String hypothesis) {
        System.out.println("onPartialResult方法被调用");
        System.out.println(hypothesis);
    }

    @Override
    public void onResult(String hypothesis) {
        System.out.println("onResult方法被调用");
        System.out.println(hypothesis);
        Gson gson = new Gson();
        results result = gson.fromJson(hypothesis, results.class);

        if(result==null) {
            Log.d(TAG,"转换失败，内容为空");
            return;
        };

        if(result.getResult()==null) {
            Log.d(TAG,"转换失败，内容为空");
            return;
        };

        try {
            for (org.vosk.demo.entity.partialResult partialResult:result.getResult()) {
                double conf = partialResult.getConf();
                words.add(partialResult.getWord());
                confs.add(conf);
                wordsConf.add(new Pair<>(partialResult.getWord(),conf));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        sentence_read.append(result.getText()).append(" ");
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onFinalResult(String hypothesis) {

        Log.d(TAG,"onFinalResult方法被调用");

        if (speechStreamService != null) {
            speechStreamService = null;
        }
        Log.d(TAG,hypothesis);

        Gson gson = new Gson();
        results result = gson.fromJson(hypothesis, results.class);

        if(result!=null&&result.getResult()!=null){
            try {
                for (org.vosk.demo.entity.partialResult partialResult:result.getResult()) {
                    double conf = partialResult.getConf();
                    words.add(partialResult.getWord());
                    confs.add(conf);
                    wordsConf.add(new Pair<>(partialResult.getWord(),conf));
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            sentence_read.append(result.getText()).append(" ");
        }
        Message msg = Message.obtain();
        msg.what=1;
        handler.sendMessage(msg);
    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void onTimeout() {

    }
}
