package com.hkhamm;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;

import java.util.concurrent.Future;

public class Main {

    private String url;

    public Main(String url) {
        this.url = url;
    }

    public void send(String message) {
        AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();
        // TODO add a body
        Future<Response> future = asyncHttpClient.preparePost(url).addBodyPart();
    }
}
