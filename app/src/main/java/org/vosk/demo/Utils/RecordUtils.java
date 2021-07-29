package org.vosk.demo.Utils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;

public class RecordUtils {

    // 音频源：音频输入-麦克风
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;

    // 采样率
    // 44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    // 采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
    private final static int AUDIO_SAMPLE_RATE = 16000;

    // 音频通道 单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;

    // 音频格式：PCM编码
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * 需要申请的运行时权限
     */
    private String[] permissions = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    private static final int MY_PERMISSIONS_REQUEST = 1001;
    private final String pcmFileName = Environment.getExternalStorageDirectory() + "/Download/record.pcm";
    private final String wavFileName = Environment.getExternalStorageDirectory() + "/Download/record.wav";
    private AudioRecord audioRecord = null;  // 声明 AudioRecord 对象
    private int recordBufSize = 0; // 声明recoordBufffer的大小字段
    private byte[] buffer;
    private boolean isRecording;

    public RecordUtils() {
    }

    public void stopRecord(){
        isRecording = false;

        if (null != audioRecord) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        Pcm2WavUtil.pcmToWav(AUDIO_SAMPLE_RATE,AUDIO_CHANNEL,recordBufSize,pcmFileName,wavFileName);
    }

    public void startRecord(){

        //audioRecord能接受的最小的buffer大小
        recordBufSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING);
        audioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, recordBufSize);
        buffer = new byte[recordBufSize];

        audioRecord.startRecording();
        isRecording = true;

        new Thread(() -> {
            FileOutputStream os = null;

            try {
                if(!new File(pcmFileName).exists()){
                    new File(pcmFileName).createNewFile();
                }
                os = new FileOutputStream(pcmFileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (null != os) {
                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, recordBufSize);

                    // 如果读取音频数据没有出现错误，就将数据写入到文件
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        try {
                            os.write(buffer);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}