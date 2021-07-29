package org.vosk.demo.Utils;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class move{

    public static void copyFilesFromAssets(Context context,String oldPath,String newPath) {
        try {
            String fileNames[] = context.getAssets().list(oldPath);//获取assets目录下的所有文件及目录名
            if (fileNames.length > 0) {//如果是目录
                File file = new File(newPath);
                file.mkdirs();//如果文件夹不存在，则递归
                for (String fileName : fileNames) {
                    copyFilesFromAssets(context,oldPath + "/" + fileName,newPath+"/"+fileName);
                }
            } else {//如果是文件
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount=0;
                while((byteCount=is.read(buffer))!=-1) {//循环从输入流读取 buffer字节
                    fos.write(buffer, 0, byteCount);//将读取的输入流写入到输出流
                }
                fos.flush();//刷新缓冲区
                is.close();
                fos.close();
            }
        } catch (Exception e) {
// TODO Auto-generated catch block
            e.printStackTrace();
////如果捕捉到错误则通知UI线程
//MainActivity.handler.sendEmptyMessage(COPY_FALSE);
        }
    }

    /**
     * 复制assets文件夹下的文件夹到apk安装后的files文件夹中
     * @param context
     * @param folder 要复制的assets文件夹下的文件夹或文件的名字，如assets文件夹下有个文件夹是Data，则folder的值为Data
     */
    void copyFileFromAssets(Context context, String folder){
        String filesDir = context.getFilesDir().getPath();
        filesDir = filesDir + "/assets/" + folder;
        copyFilesFromAssets(context,folder,filesDir);
    }

}