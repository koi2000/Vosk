package org.vosk.demo.Utils;

import android.util.Log;

import io.microshow.rxffmpeg.RxFFmpegCommandList;
import io.microshow.rxffmpeg.RxFFmpegInvoke;

public class CutAudio {
    //    "/storage/emulated/0/1/input.mp4"
    String audioInputFilePath;
    //    "/storage/emulated/0/1/output.mp4"
    String audioOutputFilePath;
    //    "00:00:00"
    String startTime;
    //    "00:01:32"
    String endTime;
    String[] commandList;
    public CutAudio(String audioInputFilePath, String audioOutputFilePath, String startTime, String endTime) {
        this.audioInputFilePath = audioInputFilePath;
        this.audioOutputFilePath = audioOutputFilePath;
        this.startTime = startTime;
        this.endTime = endTime;
        commandList = this.setCommandList();
    }
    public String[] setCommandList() {
        RxFFmpegCommandList cmdlist = new RxFFmpegCommandList();
        cmdlist.append("-y");
        cmdlist.append("-i");
        cmdlist.append(audioInputFilePath); //"/storage/emulated/0/1/input.mp4"
        cmdlist.append("-vn");
        cmdlist.append("-acodec");
        cmdlist.append("copy");
        cmdlist.append("-ss");
        cmdlist.append(startTime); // "00:00:00"
        cmdlist.append("-t"); // "00:01:32"
        cmdlist.append(endTime);
        cmdlist.append(audioOutputFilePath);  //"/storage/emulated/0/1/input.mp4"
        return cmdlist.build();
    }
    public void run(){
        RxFFmpegInvoke.getInstance().getMediaInfo(audioInputFilePath);

        RxFFmpegInvoke.getInstance().runCommandRxJava(commandList).subscribe();
        Log.d("TAG","切");
    }
}
