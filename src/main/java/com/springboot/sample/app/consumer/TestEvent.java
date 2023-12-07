package com.springboot.sample.app.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;


public class TestEvent {
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
