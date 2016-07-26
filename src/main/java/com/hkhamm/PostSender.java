package com.hkhamm;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 */
public class PostSender {
    private static final int MAX_THREADS = 10;
    private static final int MAX_ATTEMPTS = 10;
    private static final Logger log = LoggerFactory.getLogger(PostSender.class);

    private String url;
    private LinkedBlockingQueue<PostRequest> requestQueue;
    private AsyncHttpClient asyncHttpClient;
    private int threadCount;

    public PostSender(String url) {
        this.url = url;
        requestQueue = new LinkedBlockingQueue(10);
        asyncHttpClient = new DefaultAsyncHttpClient();
        threadCount = 0;
    }

    /**
     * @param body
     */
    public void send(String body) {
        RequestBuilder builder = new RequestBuilder("POST");
        Request request = builder.setUrl(url).setBody(body).build();
        PostRequest postRequest = new PostRequest(request);
        sendRequest(postRequest);
    }

    /**
     * @param postRequest
     */
    void sendRequest(PostRequest postRequest) {
        Request request = postRequest.getRequest();
        CompletableFuture<Response> promise = asyncHttpClient.prepareRequest(request).execute().toCompletableFuture();
        promise.thenAcceptAsync(response -> handleResponse(response, postRequest));
    }

    /**
     * @param response
     * @param postRequest
     */
    void handleResponse(Response response, PostRequest postRequest) {
        if (response.getStatusCode() / 100 != 2) {
            log.info("POST to {} succeeded.", this.url);
        } else {
            log.error("POST to {} failed. Attempting to resend the request.", this.url);
            postRequest.setAttempts(postRequest.getAttempts() + 1);
            if (postRequest.getAttempts() < MAX_ATTEMPTS) {
                requestQueue.offer(postRequest);
                if (this.threadCount < MAX_THREADS) {
                    startThread();
                }
            }
        }
    }

    /**
     *
     */
    void startThread() {
        threadCount++;
        Thread thread = new Thread(() -> {
            while (true) {
                PostRequest postRequest = requestQueue.poll();
                if (postRequest != null) {
                    try {
                        Thread.sleep(postRequest.getAttempts() * 2000);
                    } catch (InterruptedException e) {
                        log.error("Error sleeping while sending a post: ", e);
                    }
                    sendRequest(postRequest);
                }
            }
        });
        thread.start();
    }
}
