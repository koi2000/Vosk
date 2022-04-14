package org.vosk.demo;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;


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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;

public class Recognizer {

    private static int num = 0;
    private static final String TAG = "Recognizer";

    private Model model;
    private SpeechStreamService speechStreamService;

    //当前需要判断的句子的原文
    private String sentence;
    private List<String> sentence_splited; //会在check方法中更新

    private StringBuilder sentence_read = new StringBuilder(); //需手动更新

    private List<Double> confs;//需手动更新
    private List<String> words;//需手动更新
    private List<Pair<String, Double>> wordsConf;
    //存储转换后的句子，符合模型的要求
    private String grammer;
    private String txt;
    private String audioPath;

    private Context that;

    private RecognitionListenerImpl recognitionListener;
    private Handler handler;
    private @SuppressLint("HandlerLeak")
    Handler handler_to;

    private float sampleRate;


    public Recognizer(Context that, String txt, String audioPath) {
        this.that = that;
        confs = new ArrayList<>();
        words = new ArrayList<>();
        this.txt = txt;
        this.audioPath = audioPath;
        this.sampleRate = 16000.0f;
        wordsConf = new ArrayList<>();
    }

    /*
    public void SampleRateConverter(String outputDirPath,String changeRate) throws EncoderException {
        this.outputDir = outputDirPath;
        ChangeAudio changeAudio = new ChangeAudio(audioPath, outputDir, changeRate);
        changeAudio.run();
        this.audioPath = outputDirPath;
    }
     */

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void build(Handler obj) throws Exception {
        handler = obj;
        try {
            recognizeFile_read();
        } catch (Exception e) {
            throw new Exception("识别出现错误");
        }
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    public void initModel() throws Exception {
        Log.d(TAG, that.toString());
        try {
            if (num == 0) {
                move.copyFilesFromAssets(that, "systemSecure", that.getExternalFilesDir("").getAbsolutePath());
                ++num;
            }
            File externalFilesDir = that.getExternalFilesDir("");
            File targetDir = new File(externalFilesDir, "model");
            String resultPath = (new File(targetDir, "model-en-us")).getAbsolutePath();
            this.model = new Model(resultPath);
            Log.d(TAG, "模型构建完毕");
        } catch (Exception e) {
            throw new Exception("初始化模型出错");
        }

    }

    @SuppressLint("HandlerLeak")
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void recognizeFile_read() {
        Log.d(TAG, "调用了recognizeFile_read");
        if (speechStreamService != null) {
            speechStreamService.stop();
            speechStreamService = null;
        } else {
            try {
                grammer = new ConverterUtils().stringToGrammer(txt);
                org.vosk.Recognizer rec = new org.vosk.Recognizer(model, sampleRate, grammer.toLowerCase());

                InputStream ais = new FileInputStream(new File(audioPath));
                //InputStream ais = new FileInputStream(new File(this.outputDir));

                //Log.d(TAG,"当前文件可用字节"+ais.available());
                System.out.println("当前文件可用字节" + ais.available());
                if (ais.skip(44) != 44) throw new IOException("File too short");

                speechStreamService = new SpeechStreamService(rec, ais, sampleRate);
            } catch (IOException e) {
                e.printStackTrace();
            }

            handler_to = new Handler() {
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
                        wordsConf = recognitionListener.getWordsConf();
                        //fluency = recognitionListener.getFluency();
                        check();
                    }
                }
            };
            recognitionListener = new RecognitionListenerImpl(speechStreamService, handler_to);
            Log.d(TAG, "开始监听");
            speechStreamService.start(recognitionListener);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void check() {
        //标准的文本
        sentence = txt;
        //读到的文本
        String read = sentence_read.toString();

        //数字转英文
        //sentence = ConvertNumberToStringUtils.convert(sentence);
        //剔除符号
        sentence = sentence.replace(",", " ");
        sentence = sentence.replace(".", " ");


        Lcs lcs = new Lcs(sentence.toLowerCase(), read.toLowerCase());

        lcs.Build();
        //得到共同的字符串
        List<String> answerCommonList = lcs.getAnswerCommonList();
        //标准字符串分开之后
        sentence_splited = new ArrayList<>();
        sentence_splited.addAll(Arrays.asList(sentence.split(" ")));

        List<String> read_list = new ArrayList<>();
        read_list.addAll(Arrays.asList(read.split(" ")));

        List<Integer> answerFirst = lcs.getAnswerFirstStringIndexs();
        List<Integer> answerSecond = lcs.getAnswerSecondStringIndexs();
        Map<String, Double> map = new HashMap<>();

        double pro_score = 0;
        int prop_num = 0;
        for (int i = 0; i < answerSecond.size(); i++) {
            int index = answerSecond.get(i);
            if (index < wordsConf.size()) {
                //map.put(wordsConf.get(index).first, wordsConf.get(index).second);
                pro_score+=wordsConf.get(index).second;
                prop_num++;
            }
        }
        if(prop_num>0){
            pro_score=pro_score*100/(double) prop_num;
        }
        pro_score = Math.min(pro_score,100.0);


        double acc_score = 0;
        if(sentence_splited.size() > 0){
            acc_score = answerSecond.size()*100/(double)sentence_splited.size();
        }
        acc_score = Math.min(100,acc_score);

        double com_score = 0;
        Integer lastIndex = 0;
        int com_num = 0;
        if (answerFirst.size() > 0) {
            lastIndex = answerFirst.get(answerFirst.size() - 1);
            if ((answerCommonList.size() * 3) <= lastIndex) {
                com_num = answerCommonList.size();
            } else {
                com_num = lastIndex;
            }
            //Log.d(TAG, answerCommonList.toString());

            if (sentence_splited.size() > 0) {
                com_score = (double) com_num*100 / sentence_splited.size();
                //com_score = 0;
            }
        }
        com_score = Math.min(100,com_score);


        /*
        for (int i=0; i<answerSecond.size();i++){
            String str = read_list.get(answerSecond.get(i));
            Double factor = confs.get(i);
            map.put(str,factor);
        }
         */
        /*while() {

        }*/
        //com_score = Math.sqrt(com_score)*10;
        //pro_score = Math.sqrt(pro_score)*10;
        //acc_score = Math.sqrt(acc_score)*10;

        double tot_score = 0.35*com_score+0.15*pro_score+acc_score*0.5;
        map.put("完整度", com_score);
        map.put("发音", pro_score);
        map.put("准确度", acc_score);
        map.put("总分为", tot_score);

        Message msg = Message.obtain();
        msg.obj = map;
        msg.what = 1;

        handler.sendMessage(msg);
    }


    /*
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void check() {
        //标准的文本
        sentence = txt;
        //读到的文本
        String read = sentence_read.toString();

        //数字转英文
        //sentence = ConvertNumberToStringUtils.convert(sentence);
        //剔除符号
        sentence = sentence.replace(",", " ");
        sentence = sentence.replace(".", " ");


        Lcs lcs = new Lcs(sentence.toLowerCase(), sentence_read.toString().toLowerCase());

        lcs.Build();
        //得到共同的字符串
        List<String> answerCommonList = lcs.getAnswerCommonList();


        sentence_splited = new ArrayList<>();
        sentence_splited.addAll(Arrays.asList(sentence.split(" ")));
        //List<String> sentence_pun_arr = new ArrayList<>(Arrays.asList(sentence_pun.split(" ")));

        //Log.d(TAG, sentence_splited.toString());

        List<Integer> answerFirst = lcs.getAnswerFirstStringIndexs();
        List<Integer> answerSecond = lcs.getAnswerSecondStringIndexs();

        Integer lastIndex = 0;
        if (answerFirst.size() != 0) {
            lastIndex = answerFirst.get(answerFirst.size() - 1);
        }


        Log.d(TAG, "开始打分");
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
        */
    /*
        int com_num = 0;
            if ((answerCommonList.size() * 3) <= lastIndex) {
            com_num = answerCommonList.size();
        } else {
            com_num = lastIndex;
        }
            Log.d(TAG, answerCommonList.toString());
        com_score = (double) com_num / sentence_splited.size();
            if (sentence_splited.size() == 0) {
            com_score = 0;
        }

        com_score = Math.min(1.0, com_score);

        Map<String, Double> map = new HashMap<>();
        //求发音
        int sum = 0;
        List<Double> conf = new ArrayList<>();
            for (int i = 0; i < answerSecond.size(); i++) {
            if (i < words.size()) {
                sum += confs.get(i);
                conf.add(confs.get(i));
            }
        }

        pro_score = Math.min(100.0, sum * 100.0 / (words.size()));
            if (words.size() == 0) {
            pro_score = 0;
        }

        //求准确度
            if (sentence_splited.size() == 0) {
            acc_score = 0;
        } else {
            acc_score = (double) (answerFirst.size()) * 100 / (sentence_splited.size() + 0.01);
            acc_score = Math.min(100.0, acc_score);
            if (answerFirst == null || sentence_splited.equals(null)) {
                acc_score = 0;
            }
        }


        //求总分
        tot_score = acc_score * 0.5 + pro_score * com_score * 0.5;

        tot_score = Math.sqrt(tot_score) * 10;
        //Log.d(TAG,"流利度为:"+acc_score);
            Log.d(TAG, "完整度为:" + com_score);
            Log.d(TAG, "发音为:" + pro_score);
            Log.d(TAG, "准确度为:" + acc_score);
            Log.d(TAG, "总分为:" + tot_score);


        //map.put("流利度",flu_score);
            map.put("完整度", com_score * 100);
            map.put("发音", pro_score);
            map.put("流利度", acc_score);
            map.put("总分为", tot_score);
        Message msg = Message.obtain();
        msg.obj = map;
        msg.what = 1;
        Map<String, Double> maps = new HashMap<>();
            for (int i=0; i<answerSecond.size();i++){
            //String str = answerSecond.get(i);
        }
                handler.sendMessage(msg);
    }
    */
}
