package com.example.common.model;

import java.util.List;

public class MtiRule {
    private String name;
    private List<Integer> requiredFields;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Integer> getRequiredFields() { return requiredFields; }
    public void setRequiredFields(List<Integer> requiredFields) { this.requiredFields = requiredFields; }
}