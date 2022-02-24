package org.vosk.demo.Utils;
/*
import android.util.Log;

import io.microshow.rxffmpeg.RxFFmpegCommandList;
import io.microshow.rxffmpeg.RxFFmpegInvoke;

public class ChangeAudio {
    String audioInputFilePath;
    String audioOutputFilePath;
    String sampleRate;

    String[] commandList;
    public ChangeAudio(String audioInputFilePath, String audioOutputFilePath, String sampleRate) {
        this.audioInputFilePath = audioInputFilePath;
        this.audioOutputFilePath = audioOutputFilePath;
        this.sampleRate = sampleRate;
        commandList = this.setCommandList();
    }
    public String[] setCommandList() {
        RxFFmpegCommandList cmdlist = new RxFFmpegCommandList();
        cmdlist.append("-y");
        cmdlist.append("-i");
        cmdlist.append(audioInputFilePath); //"/storage/emulated/0/1/input.mp4"
        cmdlist.append("-ar");
        cmdlist.append(sampleRate);
        cmdlist.append(audioOutputFilePath);  //"/storage/emulated/0/1/input.mp4"
        return cmdlist.build();
    }

    public void run(){
        //RxFFmpegInvoke.getInstance().getMediaInfo(audioInputFilePath);
        RxFFmpegInvoke.getInstance().runCommandRxJava(commandList).subscribe();
        Log.d("TAG","切");
    }
}
 */