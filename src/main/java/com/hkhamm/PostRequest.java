package com.hkhamm;


import org.asynchttpclient.Request;

/**
 * Keeps information necessary for resending POST requests after failure.
 */
public class PostRequest {

    private int attempts;
    private int waitTime;
    private Request request;

    public PostRequest(Request request) {
        this.request = request;
        this.attempts = 0;
        this.waitTime = 1000;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
        this.waitTime *= 2;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public Request getRequest() {
        return request;
    }
}
