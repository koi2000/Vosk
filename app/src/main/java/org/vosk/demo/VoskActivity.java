package org.vosk.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VoskActivity extends Activity implements RecognitionListener {
    private final static String TAG = "VoskActivity";

    static private final int STATE_START = 0;
    static private final int STATE_READY = 1;
    static private final int STATE_DONE = 2;
    static private final int STATE_FILE = 3;
    static private final int STATE_MIC = 4;

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private Model model;
    private SpeechService speechService;
    private SpeechStreamService speechStreamService;
    private TextView resultView;

    //当前需要判断的句子的原文
    private String sentence;
    private List<String> sentence_splited; //会在check方法中更新

    private StringBuilder sentence_read = new StringBuilder(); //需手动更新

    private List<Double>confs;//需手动更新
    private List<String>words;//需手动更新

    //存储转换后的句子，符合模型的要求
    private String grammer;

    //获取麦克风
    private MediaRecorder mediaRecorder;
    //设置文件保存位置
    //File file;

    //控件
    Button btn_mic;

    private static int fluency = 1;

    private String txtName = "page1.txt";
    private String audioName = "A00001_page1.wav";

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.main);

        // Setup layout
        resultView = findViewById(R.id.result_text);
        setUiState(STATE_START);

        findViewById(R.id.recognize_file).setOnClickListener(view -> recognizeFile());

        btn_mic = findViewById(R.id.recognize_mic);
        btn_mic.setOnClickListener(view -> recognizeMicrophone());


        LibVosk.setLogLevel(LogLevel.INFO);

        confs = new ArrayList<>();
        words = new ArrayList<>();
        //file = new File(new File(getExternalFilesDir(""),"a.wav").getAbsolutePath());
        // Check if user has given permission to record audio, init the model after permission is granted
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            initModel();
        }
    }

    private void initModel() {
        StorageService.unpack(this, "model-en-us", "model",
                (model) -> {
                    this.model = model;
                    setUiState(STATE_READY);
                },
                (exception) -> setErrorState("Failed to unpack the model" + exception.getMessage()));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                initModel();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }

        if (speechStreamService != null) {
            speechStreamService.stop();
        }
    }

    @Override
    public void onResult(String hypothesis) {

        Log.d(TAG,"onResult方法被调用");
        Log.d(TAG,hypothesis);

        fluency++;

        Gson gson = new Gson();
        results result = gson.fromJson(hypothesis, results.class);

        if(result==null) {
            Log.d(TAG,"转换失败，啥也没有");
            return;
        };
        if(result.getResult()==null) {
            Log.d(TAG,"result里是空的");
            return;
        };

        try {
            for (partialResult partialResult:result.getResult()) {
                double conf = partialResult.getConf();
                Log.d(TAG,conf+"");
                words.add(partialResult.getWord());
                confs.add(conf);
                /*
                if(mapper.containsKey(partialResult.getWord())) {
                    //目前没有想到什么好的办法，所以目前只是将置信度设的高一点
                    mapper.put(partialResult.getWord(),Math.max(conf,mapper.get(partialResult.getWord())));
                }else {
                    mapper.put(partialResult.getWord(),partialResult.getConf());
                }
                 */
            }
        }catch (Exception e){
            Log.d(TAG,"出现了异常，没存进去");
            e.printStackTrace();
        }
        Log.d(TAG,result.getText());
        sentence_read.append(result.getText());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onFinalResult(String hypothesis) {

        Log.d(TAG,"onFinalResult方法被调用");

        setUiState(STATE_DONE);
        if (speechStreamService != null) {
            speechStreamService = null;
        }
        Log.d(TAG,hypothesis);

        Gson gson = new Gson();
        results result = gson.fromJson(hypothesis, results.class);


        if(result!=null&&result.getResult()!=null){
            try {
                for (partialResult partialResult:result.getResult()) {
                    double conf = partialResult.getConf();
                    words.add(partialResult.getWord());
                    confs.add(conf);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            sentence_read.append(result.getText());
        }
        show();
    }

    @Override
    public void onPartialResult(String hypothesis) {
        Log.d(TAG,"onPartialResult方法被调用");

        Log.d(TAG,hypothesis);
    }

    @Override
    public void onError(Exception e) {
        setErrorState(e.getMessage());
    }

    @Override
    public void onTimeout() {
        setUiState(STATE_DONE);
    }

    private void setUiState(int state) {
        switch (state) {
            case STATE_START:
                resultView.setText(R.string.preparing);
                resultView.setMovementMethod(new ScrollingMovementMethod());
                findViewById(R.id.recognize_file).setEnabled(false);
                findViewById(R.id.recognize_mic).setEnabled(false);
                //findViewById(R.id.pause).setEnabled((false));
                break;
            case STATE_READY:
                resultView.setText(R.string.ready);
                //((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
                findViewById(R.id.recognize_file).setEnabled(true);
                findViewById(R.id.recognize_mic).setEnabled(true);
                //findViewById(R.id.pause).setEnabled((false));
                resultView.append("\n");
                try {
                    resultView.append(new ConverterUtils().fileTostring(txtName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case STATE_DONE:
                //((Button) findViewById(R.id.recognize_file)).setText(R.string.recognize_file);
                //((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
                findViewById(R.id.recognize_file).setEnabled(true);
                findViewById(R.id.recognize_mic).setEnabled(true);
                //findViewById(R.id.pause).setEnabled((false));
                break;
            case STATE_FILE:
                //((Button) findViewById(R.id.recognize_file)).setText(R.string.stop_file);
                resultView.setText(getString(R.string.starting));
                findViewById(R.id.recognize_mic).setEnabled(false);
                findViewById(R.id.recognize_file).setEnabled(true);
                //findViewById(R.id.pause).setEnabled((false));
                break;
            case STATE_MIC:
                //((Button) findViewById(R.id.recognize_mic)).setText(R.string.stop_microphone);
                resultView.setText(getString(R.string.say_something));
                findViewById(R.id.recognize_file).setEnabled(false);
                findViewById(R.id.recognize_mic).setEnabled(true);
                //findViewById(R.id.pause).setEnabled((true));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }
    }

    private void setErrorState(String message) {
        resultView.setText(message);
        ((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
        findViewById(R.id.recognize_file).setEnabled(false);
        findViewById(R.id.recognize_mic).setEnabled(false);
    }

    private void recognizeFile() {

        //每次重新卢新或者上传文件之前都要把原来的数据清空
        confs = new ArrayList<>();
        words = new ArrayList<>();
        sentence_read = new StringBuilder();

        /*
        if (speechStreamService != null) {
            setUiState(STATE_DONE);
            speechStreamService.stop();
            speechStreamService = null;
        } else {
            setUiState(STATE_FILE);
            try {
                //String s = "[\"Injuries are part of everyday life\",\" from a scratch on the skin to a broken bone to a fatal trauma\",\" \",\"Although many injuries are accidental\",\" others can arise as a consequence of an individual\",\"s or a group\",\"s behaviour\",\" activity or social norms\",\" \",\" characteristics that tell us about societies and the inherent tensions and risks within and between different groups\",\"On page \",\" Beier et al\",\" provide evidence that challenges the long\",\"standing view\",\" that Neanderthal populations experienced a level of traumatic injuries that was significantly higher than that of humans\",\" The result calls into question claims that the behaviour and technologies \",\"of Neanderthals exposed them to particularly high levels of risk and danger\",\"Reports of injuries and deaths are constantly in the news\",\" As well as being drawn to read the stories of individuals\",\" such information is of interest because of what it tells us about our societies\",\" However\",\" to fully understand what might determine the current degree of violence and injuries\",\" \",\"we also need to look back at the past and identify the causal under pinnings\",\" But how far back should we look\",\" \",\"Arguably\",\" right back to the evolutionary origins of processes that shape behavioural\",\" social and cognitive tendencies and abilities\",\" \",\"Anthropologists study skeletal remains to reconstruct aspects of ancient lives\",\" building an \",\"osteo\",\"biography\",\" that casts light on part of the life history of an individual\",\" Skeletons preserve \",\" in the form of holes\",\" misshapen surfaces\",\" bone misalignments and secondary fractures\",\" radiating out from a point of impact \",\" a signature of the traumas that resulted in fractured\",\" cut or perforated bones\",\" even if the injuries subsequently healed\",\" \",\"Traumatic lesions have been frequently identified in Neanderthal fossils\",\" particularly in the head and neck\",\" leading to the view that higher levels of skeletal injury occurred in Neanderthal populations than in human populations\",\" However\",\" this is not so\",\" say Beier and colleagues\",\" The authors assessed published descriptions of Neanderthal and modern human fossil skulls\",\" found in Eurasia from approximately \",\" to \",\" years ago\",\"Comparing the number of injured and non\",\"injured Neanderthal and human skulls\",\" the authors report similar levels of head trauma in both groups\",\" \",\"The power of Beier and colleagues\",\" analyses lies in their study design\",\" Instead of comparing Neanderthal data with those of more\",\"recent or living human populations\",\" as previous studies have done\",\" the authors based their comparisons on humans \",\"who not only shared aspects of their environment with Neanderthals\",\" but whose fossil record also has a similar level of preservation\",\" Beier et al\",\" analyzed data for \",\" Neanderthal skulls and \",\" human skulls\",\" They gathered the data for \",\" skull bones\",\" \",\"and obtained information that ranged from \",\" bone in poorly preserved fossils to data for all \",\" bones\",\"per individual for well\",\"preserved ones\",\" In total\",\" the authors recorded trauma incidence in \",\" Neanderthal bones and \",\" human bones\",\"They also collected other information\",\" such as the percentage of each of the \",\" bones that was preserved for each individual\",\" \",\"as well as details including sex\",\" age at death and the fossil\",\"s geographic location\",\" \",\"Beier et al\",\" ran two sets of statistical analyzes \",\" one based on the presence or absence of trauma in each of the skull bones\",\" the other on individual fossil skulls as a whole\",\" \",\" to test whether there were any statistically significant differences between the prevalence of trauma in the Neanderthal and human fossils\",\" The authors also assessed whether trauma prevalence was linked to sex or age\",\" taking into account fossil preservation\",\" geographic location and possible inter action effects \",\"between the different variables\",\" The two analyses gave similar results\",\" \",\"The more complete the fossils are\",\" the more likely they are to have preserved evidence of injuries\",\" This might seem obvious\",\" but is an issue often ignored in such studies\",\" Beier et al\",\" offer a way to deal with this type of bias in the available material\",\" Once the authors take into account the extent of fossil preservation\",\" \",\"the predicted prevalence of trauma in Neanderthals and humans is almost the same\",\" \",\"Both Neanderthal and human males had a much greater incidence of trauma than did the females of their respective species\",\" \",\"This pattern remains the same for humans today\",\" One final intriguing result is that\",\" although traumatic injuries were present across all of the age ranges studied\",\" Neanderthals that had trauma to the head were more likely to have died under the age of \",\" than the humans were\",\" The authors interpret this result as evidence that\",\" compared with humans\",\" Neanderthals either had more injuries\",\" when they were young or were more likely to have died after being injured\",\"   \",\"Beier and colleagues\",\" study does not invalidate previous estimates of trauma among Neanderthals\",\" Instead\",\" it provides a new framework for interpreting these data by showing that the level of Neanderthal trauma was not uniquely\",\" high relative to that of early humans in Eurasia\",\" \",\"This implies that Neanderthal trauma does not require its own special explanations\",\" and that risk and danger were as much a part of the life of Neanderthals \",\"as they were of our own evolutionary past\",\" The result adds to growing evidence that Neanderthals had much in common with early human groups\",\" However\",\" the finding that Neanderthals might have experienced trauma at a younger age than humans\",\" or that they had a greater risk of death after injury\",\" is fascinating\",\" and might be a key insight into why our species had such a demographic advantage over Neanderthals\",\"Is this the final word on the subject of Neanderthal trauma\",\" The answer is no\",\" Beier and colleagues assessed only skull trauma\",\" \",\"What if Neanderthals accumulated more injuries to their bodies than did humans\",\" There are data suggesting that this might be the case\",\" \",\"Furthermore\",\" although the authors\",\" analyzes demonstrate the power of a well\",\"designed study based on large samples\",\" \",\"the data they used were recorded by many researchers and at varying levels of detail\",\" raising the possibility of methodological biases\",\" \",\"Lastly\",\" the causes of the injuries could provide some elusive insights into behaviour\",\" activities or social norms in the past\",\" From the shape\",\" location and extent of traumatic injuries in skeletons\",\" and characteristics such as the sharpness of fracture edges or the degree to which injuries had healed\",\" it is sometimes possible to establish the most likely cause of a trauma\",\" \",\" for example\",\" whether the injury probably arose as a consequence of a hunting accident\",\" \",\"interpersonal violence or inter\",\"group conflict\",\" Moreover\",\" surviving severe trauma might indicate that the injured person was cared for by members of their society\",\" Establishing the likelihood of each of these scenarios among Neanderthals and early modern humans\",\" will no doubt continue to challenge scientists for many years to come\", \"[unk]\"]";

                grammer =new ConverterUtils().fileTogrammer(txtName);
                Recognizer rec = new Recognizer(model, 16000.f, grammer);

                InputStream ais = getAssets().open(audioName);
                //InputStream ais = new FileInputStream(file);

                Log.d(TAG,"当前文件可用字节"+ais.available());
                if (ais.skip(44) != 44) throw new IOException("File too short");

                speechStreamService = new SpeechStreamService(rec, ais, 16000.f);
                speechStreamService.start(this);
            } catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }

         */
    }

    /*
    private void recognizeFile_read() {

        //每次重新卢新或者上传文件之前都要把原来的数据清空
        mapper.clear();
        sentence_read = new StringBuilder();


        if (speechStreamService != null) {
            setUiState(STATE_DONE);
            speechStreamService.stop();
            speechStreamService = null;
        } else {
            setUiState(STATE_FILE);
            try {
                //String s = "[\"Injuries are part of everyday life\",\" from a scratch on the skin to a broken bone to a fatal trauma\",\" \",\"Although many injuries are accidental\",\" others can arise as a consequence of an individual\",\"s or a group\",\"s behaviour\",\" activity or social norms\",\" \",\" characteristics that tell us about societies and the inherent tensions and risks within and between different groups\",\"On page \",\" Beier et al\",\" provide evidence that challenges the long\",\"standing view\",\" that Neanderthal populations experienced a level of traumatic injuries that was significantly higher than that of humans\",\" The result calls into question claims that the behaviour and technologies \",\"of Neanderthals exposed them to particularly high levels of risk and danger\",\"Reports of injuries and deaths are constantly in the news\",\" As well as being drawn to read the stories of individuals\",\" such information is of interest because of what it tells us about our societies\",\" However\",\" to fully understand what might determine the current degree of violence and injuries\",\" \",\"we also need to look back at the past and identify the causal under pinnings\",\" But how far back should we look\",\" \",\"Arguably\",\" right back to the evolutionary origins of processes that shape behavioural\",\" social and cognitive tendencies and abilities\",\" \",\"Anthropologists study skeletal remains to reconstruct aspects of ancient lives\",\" building an \",\"osteo\",\"biography\",\" that casts light on part of the life history of an individual\",\" Skeletons preserve \",\" in the form of holes\",\" misshapen surfaces\",\" bone misalignments and secondary fractures\",\" radiating out from a point of impact \",\" a signature of the traumas that resulted in fractured\",\" cut or perforated bones\",\" even if the injuries subsequently healed\",\" \",\"Traumatic lesions have been frequently identified in Neanderthal fossils\",\" particularly in the head and neck\",\" leading to the view that higher levels of skeletal injury occurred in Neanderthal populations than in human populations\",\" However\",\" this is not so\",\" say Beier and colleagues\",\" The authors assessed published descriptions of Neanderthal and modern human fossil skulls\",\" found in Eurasia from approximately \",\" to \",\" years ago\",\"Comparing the number of injured and non\",\"injured Neanderthal and human skulls\",\" the authors report similar levels of head trauma in both groups\",\" \",\"The power of Beier and colleagues\",\" analyses lies in their study design\",\" Instead of comparing Neanderthal data with those of more\",\"recent or living human populations\",\" as previous studies have done\",\" the authors based their comparisons on humans \",\"who not only shared aspects of their environment with Neanderthals\",\" but whose fossil record also has a similar level of preservation\",\" Beier et al\",\" analyzed data for \",\" Neanderthal skulls and \",\" human skulls\",\" They gathered the data for \",\" skull bones\",\" \",\"and obtained information that ranged from \",\" bone in poorly preserved fossils to data for all \",\" bones\",\"per individual for well\",\"preserved ones\",\" In total\",\" the authors recorded trauma incidence in \",\" Neanderthal bones and \",\" human bones\",\"They also collected other information\",\" such as the percentage of each of the \",\" bones that was preserved for each individual\",\" \",\"as well as details including sex\",\" age at death and the fossil\",\"s geographic location\",\" \",\"Beier et al\",\" ran two sets of statistical analyzes \",\" one based on the presence or absence of trauma in each of the skull bones\",\" the other on individual fossil skulls as a whole\",\" \",\" to test whether there were any statistically significant differences between the prevalence of trauma in the Neanderthal and human fossils\",\" The authors also assessed whether trauma prevalence was linked to sex or age\",\" taking into account fossil preservation\",\" geographic location and possible inter action effects \",\"between the different variables\",\" The two analyses gave similar results\",\" \",\"The more complete the fossils are\",\" the more likely they are to have preserved evidence of injuries\",\" This might seem obvious\",\" but is an issue often ignored in such studies\",\" Beier et al\",\" offer a way to deal with this type of bias in the available material\",\" Once the authors take into account the extent of fossil preservation\",\" \",\"the predicted prevalence of trauma in Neanderthals and humans is almost the same\",\" \",\"Both Neanderthal and human males had a much greater incidence of trauma than did the females of their respective species\",\" \",\"This pattern remains the same for humans today\",\" One final intriguing result is that\",\" although traumatic injuries were present across all of the age ranges studied\",\" Neanderthals that had trauma to the head were more likely to have died under the age of \",\" than the humans were\",\" The authors interpret this result as evidence that\",\" compared with humans\",\" Neanderthals either had more injuries\",\" when they were young or were more likely to have died after being injured\",\"   \",\"Beier and colleagues\",\" study does not invalidate previous estimates of trauma among Neanderthals\",\" Instead\",\" it provides a new framework for interpreting these data by showing that the level of Neanderthal trauma was not uniquely\",\" high relative to that of early humans in Eurasia\",\" \",\"This implies that Neanderthal trauma does not require its own special explanations\",\" and that risk and danger were as much a part of the life of Neanderthals \",\"as they were of our own evolutionary past\",\" The result adds to growing evidence that Neanderthals had much in common with early human groups\",\" However\",\" the finding that Neanderthals might have experienced trauma at a younger age than humans\",\" or that they had a greater risk of death after injury\",\" is fascinating\",\" and might be a key insight into why our species had such a demographic advantage over Neanderthals\",\"Is this the final word on the subject of Neanderthal trauma\",\" The answer is no\",\" Beier and colleagues assessed only skull trauma\",\" \",\"What if Neanderthals accumulated more injuries to their bodies than did humans\",\" There are data suggesting that this might be the case\",\" \",\"Furthermore\",\" although the authors\",\" analyzes demonstrate the power of a well\",\"designed study based on large samples\",\" \",\"the data they used were recorded by many researchers and at varying levels of detail\",\" raising the possibility of methodological biases\",\" \",\"Lastly\",\" the causes of the injuries could provide some elusive insights into behaviour\",\" activities or social norms in the past\",\" From the shape\",\" location and extent of traumatic injuries in skeletons\",\" and characteristics such as the sharpness of fracture edges or the degree to which injuries had healed\",\" it is sometimes possible to establish the most likely cause of a trauma\",\" \",\" for example\",\" whether the injury probably arose as a consequence of a hunting accident\",\" \",\"interpersonal violence or inter\",\"group conflict\",\" Moreover\",\" surviving severe trauma might indicate that the injured person was cared for by members of their society\",\" Establishing the likelihood of each of these scenarios among Neanderthals and early modern humans\",\" will no doubt continue to challenge scientists for many years to come\", \"[unk]\"]";

                grammer = new ConverterUtils().fileTogrammer(txtName);
                Recognizer rec = new Recognizer(model, 128000.f, grammer);

                //InputStream ais = getAssets().open("A00001.wav");
                InputStream ais = new FileInputStream(file);

                Log.d(TAG,"当前文件可用字节"+ais.available());
                if (ais.skip(44) != 44) throw new IOException("File too short");

                speechStreamService = new SpeechStreamService(rec, ais, 128000.f);

                speechStreamService.start(this);

            } catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }

     */

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void recognizeMicrophone() {

        fluency = 1;
        if(TextUtils.equals(btn_mic.getText(),"StartRecord")){
            btn_mic.setText("Stop");

            confs = new ArrayList<>();
            words = new ArrayList<>();
            sentence_read = new StringBuilder();
            if (speechService != null) {
                setUiState(STATE_DONE);
                speechService.stop();
                speechService = null;
            }
            try {
                Recognizer rec = new Recognizer(model, 44100.0f);
                speechService = new SpeechService(rec, 44100.0f);
                speechService.startListening(this);
            } catch (IOException e) {
                setErrorState(e.getMessage());
            }

        }else {
            btn_mic.setText("StartRecord");
            speechService.stop();
            speechService.shutdown();

        }

        /*
        if(TextUtils.equals(btn_mic.getText(),"StartRecord")){
            btn_mic.setText("Stop");
            mediaRecorder = new MediaRecorder();


            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); //设置音频源 麦克风
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);//指定视频文件格式
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            mediaRecorder.setOutputFile(file);

            try {
                mediaRecorder.prepare();
            } catch (IOException e) {
                Log.d(TAG,"prepare失败");
                e.printStackTrace();
            }
            try{
                mediaRecorder.start();
            }catch (Exception e){
                Log.d(TAG,"start失败");
                e.printStackTrace();
            }
        }else {
            btn_mic.setText("StartRecord");
            mediaRecorder.stop();
            mediaRecorder.release();
            Log.d(TAG,"音频流被释放了");

            recognizeFile_read();
            Log.d(TAG,"开始检测");
        }

         */
    }


    private void pause(boolean checked) {
        if (speechService != null) {
            speechService.setPause(checked);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void show(){
        Log.d(TAG,"show方法被调用");
        try {
            sentence = new ConverterUtils().fileTostring(txtName);
            Log.d(TAG,"转换成功");
        } catch (IOException e) {
            Log.d(TAG,"转换失败");
            e.printStackTrace();
        }
        String read = sentence_read.toString();

        Log.d(TAG,"confs里有"+confs.toString());
        Log.d(TAG,"words里有"+words.toString());
        Log.d(TAG,"读到了"+read);
//        数字转英文
        sentence = ConvertNumberToStringUtils.convert(sentence);
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


        resultView.setText("");

        Integer punctuationNum = 0;
        Integer lastIndex = 0;
        if(answerFirst.size()!=0){
            lastIndex = answerFirst.get(answerFirst.size() - 1);
        }

        for (int i = 0; i < sentence_splited.size(); i++) {
            String temp = sentence_splited.get(i);
            int index = -1;
            boolean isPunctuation = false;

            if ((i<=lastIndex)&&(temp.equals(",")||temp.equals("."))){
                isPunctuation=true;
                punctuationNum++;
            }
            SpannableString msp = msp = new SpannableString(temp+" ");
            if (words.contains(temp)){
                index = words.indexOf(temp);
            }
            if(answerFirst.contains(i)){
                try{
                    if(index!=-1){
                        if(confs.get(index)<0.7){
                            msp.setSpan(new ForegroundColorSpan(Color.YELLOW),
                                    0,msp.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }else {
                            msp.setSpan(new ForegroundColorSpan(Color.GREEN),
                                    0,msp.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        confs.remove(index);
                        words.remove(index);
                    }else {
                        msp.setSpan(new ForegroundColorSpan(Color.GREEN),
                                0,msp.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                }catch (Exception e){
                    Log.d(TAG,"出现了异常");
                    msp.setSpan(new ForegroundColorSpan(Color.GREEN),
                            0,msp.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }else {
                msp.setSpan(new ForegroundColorSpan(Color.RED),
                        0,msp.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (isPunctuation){
                msp.setSpan(new ForegroundColorSpan(Color.GREEN),
                        0,msp.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            resultView.append(msp);
        }

        /*
        SpannableString msp = new SpannableString(sentence);

        msp.setSpan(new ForegroundColorSpan(Color.RED),
                0,msp.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        for (int i = 0;i<answerFirst.size();i++){
            msp.setSpan(new ForegroundColorSpan(Color.GREEN),
                    answerFirst.get(i),answerSecond.get(i),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
         */
        /*
        SpannableString msp = null;
        for (int i=0;i<states.size();i++){

            msp = new SpannableString(sentence_splited.get(i)+" ");
            if(states.get(i)==0){
                msp.setSpan(new ForegroundColorSpan(Color.GREEN),
                        0,msp.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }else if(states.get(i)==1){
                msp.setSpan(new ForegroundColorSpan(Color.YELLOW),
                        0,msp.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }else {
                msp.setSpan(new ForegroundColorSpan(Color.RED),
                        0,msp.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            resultView.append(msp);

        }

         */

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
        //tot_score+=

        //求完整度

        Log.d(TAG,answerCommonList.toString());
        com_score = (double)Math.min(lastIndex,answerCommonList.size())*100/sentence_splited.size();

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
        resultView.append("\n"+"流利度："+(int)Math.floor(flu_score));
        resultView.append("\n"+"完整度："+(int)Math.floor(com_score));
        resultView.append("\n"+"发音："+(int)Math.floor(pro_score));
        resultView.append("\n"+"准确度："+(int)Math.floor(acc_score));
        resultView.append("\n"+"总分："+(int)Math.floor(tot_score));
        Log.d(TAG,"打分结束");
    }

}
