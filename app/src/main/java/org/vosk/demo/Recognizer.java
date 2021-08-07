package org.vosk.demo;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


import androidx.annotation.RequiresApi;

import com.google.gson.Gson;

import org.vosk.Model;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechStreamService;
import org.vosk.demo.Utils.ConverterUtils;
import org.vosk.demo.Utils.Lcs;
import org.vosk.demo.Utils.RecognitionListenerImpl;
import org.vosk.demo.Utils.move;
import org.vosk.demo.entity.partialResult;
import org.vosk.demo.entity.results;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Recognizer{

    private static int num = 0;
    private static final String TAG = "Recognizer";

    private Model model;
    private SpeechStreamService speechStreamService;

    //当前需要判断的句子的原文
    private String sentence;
    private List<String> sentence_splited; //会在check方法中更新

    private StringBuilder sentence_read = new StringBuilder(); //需手动更新

    private List<Double>confs;//需手动更新
    private List<String>words;//需手动更新
    //存储转换后的句子，符合模型的要求
    private String grammer;
    private String txt;
    private String audioPath;

    private Context that;
    private static double fluency = 0.1;
    private RecognitionListenerImpl recognitionListener;
    private Handler handler;
    private @SuppressLint("HandlerLeak") Handler handler_to;

    public Recognizer(Context that, String txt, String audioPath) {
        this.that = that;
        confs = new ArrayList<>();
        words = new ArrayList<>();
        this.txt = txt;
        this.audioPath = audioPath;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void build(Handler obj){
        handler=obj;
        recognizeFile_read();
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    public void initModel() {
        Log.d(TAG, that.toString());
        if (num == 0) {
            move.copyFilesFromAssets(that, "systemSecure", that.getExternalFilesDir("").getAbsolutePath());
            ++num;
        }
        File externalFilesDir = that.getExternalFilesDir("");
        File targetDir = new File(externalFilesDir, "model");
        String resultPath = (new File(targetDir, "model-en-us")).getAbsolutePath();
        this.model = new Model(resultPath);
        Log.d(TAG,"模型构建完毕");
    }

    @SuppressLint("HandlerLeak")
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void recognizeFile_read() {
        Log.d(TAG,"调用了recognizeFile_read");
        if (speechStreamService != null) {
            speechStreamService.stop();
            speechStreamService = null;
        } else {
            try {
                grammer = new ConverterUtils().stringToGrammer(txt);
                org.vosk.Recognizer rec = new org.vosk.Recognizer(model, 44100.f, grammer.toLowerCase());

                InputStream ais = new FileInputStream(new File(audioPath));

                Log.d(TAG,"当前文件可用字节"+ais.available());
                if (ais.skip(44) != 44) throw new IOException("File too short");

                speechStreamService = new SpeechStreamService(rec, ais, 44100.f);
            } catch (IOException e) {
                e.printStackTrace();
            }

            handler_to  = new Handler(){
                @RequiresApi(api = Build.VERSION_CODES.N)
                @SuppressLint("HandlerLeak")
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    //判断标志位
                    if (msg.what == 1) {
                        confs = recognitionListener.getConfs();
                        words = recognitionListener.getWords();
                        sentence_read = recognitionListener.getSentence_read();
                        fluency = recognitionListener.getFluency();
                        check();
                    }
                }
            };
            recognitionListener = new RecognitionListenerImpl(speechStreamService,handler_to);
            Log.d(TAG,"开始监听");
            speechStreamService.start(recognitionListener);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void check(){
        sentence = txt;
        String sentence_pun =txt;
        String read = sentence_read.toString();

//        数字转英文
        //sentence = ConvertNumberToStringUtils.convert(sentence);
        sentence = sentence.replace(",", " ");
        sentence = sentence.replace("."," ");

        sentence_pun = sentence_pun.replace(",", " , ");
        sentence_pun = sentence_pun.replace("."," . ");

        Lcs lcs = new Lcs(sentence.toLowerCase(), sentence_read.toString().toLowerCase());

        lcs.Build();

        List<String> answerCommonList = lcs.getAnswerCommonList();


        sentence_splited = new ArrayList<>();
        sentence_splited.addAll(Arrays.asList(sentence.split(" ")));
        List<String> sentence_pun_arr = new ArrayList<>(Arrays.asList(sentence_pun.split(" ")));

        Log.d(TAG,sentence_splited.toString());

        List<Integer> answerFirst = lcs.getAnswerFirstStringIndexs();
        List<Integer> answerSecond = lcs.getAnswerSecondStringIndexs();

        Integer lastIndex = 0;
        if(answerFirst.size()!=0){
            lastIndex = answerFirst.get(answerFirst.size() - 1);
        }

        Log.d(TAG,"开始打分");
        //流利度
        double flu_score = 0;
        //完整度
        double com_score = 0;
        //发音
        double pro_score = 0;
        //准确度
        double acc_score = 0;
        //总分
        double tot_score = 0;

        //求流利度
        //查看当前读了多少句
        int num = 0;
        int boundary = lastIndex;
        for (int i=0;i<boundary;i++){
            if(sentence_pun_arr.get(i).equals(",")||sentence_pun_arr.get(i).equals(".")){
                num++;
                boundary++;
            }
        }

        flu_score+=(double)num*100/fluency;
        if (flu_score>100){
            flu_score = 100;
        }

        //求完整度
        int com_num = 0;
        if((answerCommonList.size()*3)<=lastIndex){
            com_num = answerCommonList.size();
        }else {
            com_num = lastIndex;
        }
        Log.d(TAG,answerCommonList.toString());
        com_score = (double)com_num/sentence_splited.size();
        com_score = Math.min(1.0,com_score);

        //求发音
        int sum = 0;
        for (int i = 0; i < answerSecond.size(); i++) {
            if (i < words.size()) {
                sum += confs.get(i);
            }
        }
        pro_score = Math.min(100.0,sum*100.0/(words.size()+0.01));

        //求准确度
        if(sentence_splited.size()==0){
            acc_score = 0;
        }else {
            acc_score = (double) (answerFirst.size())*100/(sentence_splited.size()+0.01);
            acc_score = Math.min(100.0,acc_score);
        }

        //求总分
        tot_score = acc_score*0.5+(pro_score+flu_score)*0.5*com_score*0.5;

        tot_score = Math.sqrt(tot_score)*10;
        Log.d(TAG,"流利度为:"+flu_score);
        Log.d(TAG,"完整度为:"+com_score);
        Log.d(TAG,"发音为:"+pro_score);
        Log.d(TAG,"准确度为:"+acc_score);
        Log.d(TAG,"总分为:"+tot_score);

        Message msg = Message.obtain();
        msg.obj = (int)tot_score;
        msg.what=1;
        handler.sendMessage(msg);
    }
}
