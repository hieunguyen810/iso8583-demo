package com.example.common.model;

public class FieldRule {
    private String name;
    private String type;
    private String format;
    private Integer length;
    private Integer maxLength;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Integer getLength() { return length; }
    public void setLength(Integer length) { this.length = length; }

    public Integer getMaxLength() { return maxLength; }
    public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }
}