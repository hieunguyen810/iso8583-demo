package com.example.demo.model;

import java.util.HashMap;
import java.util.Map;

public class Iso8583Message {
    private String mti;
    private Map<Integer, String> fields = new HashMap<>();
    
    public void setMti(String mti) {
        this.mti = mti;
    }
    
    public String getMti() {
        return mti;
    }
    
    public void addField(int fieldNumber, String value) {
        fields.put(fieldNumber, value);
    }
    
    public String getField(int fieldNumber) {
        return fields.get(fieldNumber);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(mti);
        
        for (Map.Entry<Integer, String> entry : fields.entrySet()) {
            sb.append("|").append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        return sb.toString();
    }
}