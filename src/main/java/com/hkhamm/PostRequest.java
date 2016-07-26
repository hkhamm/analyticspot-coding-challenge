package com.hkhamm;


import org.asynchttpclient.Request;

public class PostRequest {

    private int attempts;
    private Request request;

    public PostRequest(Request request) {
        this.request = request;
        this.attempts = 0;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }
}
