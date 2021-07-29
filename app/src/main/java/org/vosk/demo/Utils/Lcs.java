package org.vosk.demo.Utils;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Lcs {
    private int length;
    private int[][] dp =null;
    private int[][] flag =null;
    private List<String> answer,s1,s2;
    private List<Integer>first,second;
    private int commonLength;

    public Lcs(String a, String b){
        s1 = init(a);
        s2 = init(b);
        length = Math.max(s1.size(),s2.size())+1;
        dp =new int[length][length];
        flag =new int[length][length];
        answer=new ArrayList<>();
        first=new ArrayList<>();
        second = new ArrayList<>();
        for(int i=0;i<length;i++)
            for(int j=0;j<length;j++){
                dp[i][j]=0;
                flag[i][j]=0;
            }
        answer.clear();
        commonLength = 0;
    }

    private List<String> init(String s){
        //s = s.replace(",", " , ");
        //s = s.replace("."," . ");
        List<String>lStrings = new ArrayList<>();
        String[] strings =  s.split(" ");

        for(int i=0;i<strings.length;i++)
        {
            lStrings.add(strings[i]);
        }
        return lStrings;
    }

    private int getLcs(){

        List<String>x = s1;
        List<String>y = s2;
        for(int i=0;i<x.size();i++)
        {
            for(int j=0;j<y.size();j++)
            {
                if(x.get(i).equals(y.get(j)) ){
                    dp[i+1][j+1] = dp[i][j]+1;
                    flag[i+1][j+1]=0;
                }
                else if(dp[i][j+1]>=dp[i+1][j]){
                    dp[i+1][j+1]=dp[i][j+1];
                    flag[i+1][j+1]=1;
                }
                else {
                    dp[i+1][j+1]=dp[i+1][j];
                    flag[i+1][j+1]=-1;
                }
            }
        }
        return dp[x.size()][y.size()];
    }

    private void printLcs(int i,int j){
        if(i==0||j==0){
            return;
        }
        if(flag[i][j]==0){
            printLcs(i-1, j-1);
            answer.add(s1.get(i-1));
            first.add(i-1);
            second.add(j-1);
        }
        else if(flag[i][j]==1){
            printLcs(i-1, j);
        }
        else{
            printLcs(i, j-1);
        }
    }

    public List<String> getAnswerCommonList(){
        return answer;
    }
    public int getAnswerCommonLength(){
        return commonLength;
    }
    public List<Integer>getAnswerFirstStringIndexs(){
        return first;
    }
    public List<Integer>getAnswerSecondStringIndexs(){
        return second;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public Lcs Build(){
        commonLength = getLcs();
        printLcs(s1.size(), s2.size());
        first.sort(Comparator.comparingInt(Integer::intValue));
        second.sort(Comparator.comparingInt(Integer::intValue));
        return this;
    }
}
