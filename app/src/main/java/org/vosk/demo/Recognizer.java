package org.vosk.demo;

import android.app.Activity;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;

import org.vosk.Model;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechStreamService;
import org.vosk.demo.Utils.ConverterUtils;
import org.vosk.demo.Utils.Lcs;
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

public class Recognizer extends Activity implements RecognitionListener {

    private static int num = 0;
    private static final String TAG = "Recognizer";

    private Model model;
    private SpeechStreamService speechStreamService;

    //当前需要判断的句子的原文
    private String sentence;
    private List<String> sentence_splited; //会在check方法中更新

    private StringBuilder sentence_read = new StringBuilder(); //需手动更新

    private List<partialResult>partialResults;
    private List<Double>confs;//需手动更新
    private List<String>words;//需手动更新
    //存储转换后的句子，符合模型的要求
    private String grammer;
    private String txt;
    private String audioPath;

    public Recognizer(String txt, String audioPath) {
        initModel();
        confs = new ArrayList<>();
        words = new ArrayList<>();
        this.txt = txt;
        this.audioPath = audioPath;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public int getScore(){
        try{
            recognizeFile_read();
            return check();
        }catch (Exception e){
            return -1;
        }

    }


    private void initModel() {
        if(num==0){
            move.copyFilesFromAssets(this, "systemSecure",this.getExternalFilesDir("").getAbsolutePath());
            num++;
        }
        File externalFilesDir = this.getExternalFilesDir((String)null);
        File targetDir = new File(externalFilesDir, "model");
        String resultPath = (new File(targetDir, "model-en-us")).getAbsolutePath();

        model = new Model(resultPath);
    }

    private static double fluency = 0.1;


    @Override
    public void onPartialResult(String hypothesis) {
        Log.d(TAG,"onPartialResult方法被调用");
        Log.d(TAG,hypothesis);
    }

    @Override
    public void onResult(String hypothesis) {
        fluency++;
        Log.d(TAG,"onResult方法被调用");
        Log.d(TAG,hypothesis);
        Gson gson = new Gson();
        results result = gson.fromJson(hypothesis, results.class);

        if(result==null) {
            Log.d(TAG,"转换失败，内容为空");
            return;
        };

        if(result.getResult()==null) {
            Log.d(TAG,"result里是空的");
            return;
        };

        try {
            for (org.vosk.demo.entity.partialResult partialResult:result.getResult()) {
                double conf = partialResult.getConf();
                words.add(partialResult.getWord());
                confs.add(conf);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        sentence_read.append(result.getText());
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
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            sentence_read.append(result.getText());
        }
    }

    private void recognizeFile_read() {
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

                speechStreamService.start(this);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private int check(){
        Log.d(TAG,"show方法被调用");
        sentence = txt;
        String read = sentence_read.toString();


//        数字转英文
        //sentence = ConvertNumberToStringUtils.convert(sentence);
        sentence = sentence.replace(",", " , ");
        sentence = sentence.replace("."," . ");
        Lcs lcs = new Lcs(sentence.toLowerCase(), sentence_read.toString().toLowerCase());

        lcs.Build();
        Log.d(TAG,"lcs build成功");

        List<String> answerCommonList = lcs.getAnswerCommonList();


        sentence_splited = new ArrayList<>();
        sentence_splited.addAll(Arrays.asList(sentence.split(" ")));

        Log.d(TAG,sentence_splited.toString());

        List<Integer> answerFirst = lcs.getAnswerFirstStringIndexs();

        int punctuationNum = 0;

        Integer lastIndex = 0;
        if(answerFirst.size()!=0){
            lastIndex = answerFirst.get(answerFirst.size() - 1);
        }

        for (int i = 0; i < sentence_splited.size(); i++) {
            if (sentence_splited.get(i).equals(",")||sentence_splited.get(i).equals(".")){
                punctuationNum++;
            }
        }

        //设置字体前景色

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
        for (int i=0;i<lastIndex;i++){
            if(sentence_splited.get(i).equals(",")||sentence_splited.get(i).equals(".")){
                num++;
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
        com_score = (double)com_num*100/sentence_splited.size();

        //求发音
        int sum = 0;
        for (Double d:confs){
            sum+=d;
        }
        pro_score = (double)sum*100/confs.size();

        //求准确度
        acc_score = (double) (answerFirst.size()+punctuationNum)*100/sentence_splited.size();

        //求总分
        tot_score = (flu_score+com_score+pro_score+acc_score)/4;

        Log.d(TAG,"打分结束");
        return (int)tot_score;
    }




    @Override
    public void onError(Exception e) {
    }

    @Override
    public void onTimeout() {
    }
}
