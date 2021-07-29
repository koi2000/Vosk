package org.vosk.demo.Utils;

import org.vosk.demo.Utils.NumberText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConvertNumberToStringUtils {
    public static String convert(String sentence) {
        NumberText nt = NumberText.getInstance(NumberText.Lang.English);
        String patten = "\\d+";
        Pattern r = Pattern.compile(patten);
        Matcher match = r.matcher(sentence);
        while (match.find()) {
            String numberInt = match.group();
            String numberStr = nt.getText(numberInt);
            sentence = sentence.replace(numberInt, numberStr);
        }
        return sentence;
    }


}
