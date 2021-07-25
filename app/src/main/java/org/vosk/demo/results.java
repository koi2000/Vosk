package org.vosk.demo;

import java.util.List;

public class results {
    public List<partialResult> getResult() {
        return result;
    }

    public String getText() {
        return text;
    }

    private List<partialResult> result;
    private String text;
}
