package org.vosk.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class checkTrue {
    //0为正确，1为不确定，2为错误
    private List<String> standardSentence;
    private List<String> testSentence;
    private List<Integer> states;
    private Map<String,Double> mapper;

    checkTrue(List<String> list1, List<String> list2, Map<String,Double> mapper) {
        states = new ArrayList<>();
        standardSentence = list1;
        testSentence = list2;
        this.mapper = mapper;
    }

    public List<Integer> getResult() {

        for (int i=0;i<standardSentence.size();i++){
            String str = standardSentence.get(i);

            if(!testSentence.contains(str)){
                states.add(i,2);
            }else {
                if(mapper.containsKey(str)){
                    if(mapper.get(str)>=0.7){
                        states.add(i,0);
                    }else {
                        states.add(i,1);
                    }
                } else {
                    states.add(i,1);
                }
            }
        }

        return states;
    }
}
