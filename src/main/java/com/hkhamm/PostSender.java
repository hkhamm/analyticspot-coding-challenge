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
 * A HTTP POST request sender. Once constructed with an {@code url} string, the {@code send} method sends the given body
 * to the {@code url}. Failed requests are re-sent up to {@code MAX_ATTEMPTS} times on up to {@code MAX_THREADS}
 * threads.
 */
public class PostSender {
    private static final int MAX_THREADS = 3;
    private static final int MAX_ATTEMPTS = 10;
    private static final Logger log = LoggerFactory.getLogger(PostSender.class);

    private String url;
    private LinkedBlockingQueue<PostRequest> requestQueue;
    private AsyncHttpClient asyncHttpClient;
    private int threadCount;
    private boolean finished;

    public PostSender(String url) {
        this.url = url;
        requestQueue = new LinkedBlockingQueue(10);
        asyncHttpClient = new DefaultAsyncHttpClient();
        threadCount = 0;
        finished = false;
    }

    /**
     * Gets the {@code threadCount}. Used to test that the count does not exceed {@code MAX_THREADS}.
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * Gets the {@code requestQueue} queue. Used to test that the queue does not exceed its size.
     */
    LinkedBlockingQueue<PostRequest> getRequestQueue() {
        return requestQueue;
    }

    /**
     * Gets the {@code finished} boolean. {@code finished} is set to true when the successful response has been received
     * or when a failed request has been re-sent {@code MAX_ATTEMPTS} times.
     */
    boolean isFinished() {
        return finished;
    }

    /**
     * Creates a request with the passed {@param body} as the body of an HTTP POST to the {@code url} with which it was
     * constructed. {@return promise} is the CompletableFuture response returned to the caller.
     */
    public CompletableFuture<Response> send(String body) {
        RequestBuilder builder = new RequestBuilder();
        Request request = builder.setUrl(url).setBody(body).setMethod("POST").build();
        PostRequest postRequest = new PostRequest(request);
        return sendRequest(postRequest);
    }

    /**
     * Sends requests and re-sends failed requests to the server at the {@code url}. The {@param postRequest} holds
     * the request to be sent. {@return promise} is the CompletableFuture response returned to the caller.
     */
    private CompletableFuture<Response> sendRequest(PostRequest postRequest) {
        Request request = postRequest.getRequest();
        log.info("Sending POST request with body \"{}\" to {}.", request.getStringData(), this.url);
        CompletableFuture<Response> promise = asyncHttpClient.prepareRequest(request).execute().toCompletableFuture();
        promise.thenAccept(response -> handleResponse(response, postRequest));
        return promise;
    }

    /**
     * Handles the POST {@param response}. If the server responds with success (any 20x response code) it is done. If
     * not, failed send attempts are retried with an exponential backoff. The first retry is attempted after 1 second,
     * then 2 seconds, then 4 seconds, up to 1024 seconds. If it fails 10 times the item is dropped. The
     * {@param postRequest} keeps the current {@code attempts} and {@code waitTime}.
     */
    private void handleResponse(Response response, PostRequest postRequest) {
        if (response.getStatusCode() / 100 == 2) {
            log.info("POST to {} succeeded.", this.url);
            finished = true;
        } else {
            log.error("POST to {} failed. Attempting to resend the request.", this.url);
            log.info("Attempts: {}", postRequest.getAttempts());
            if (postRequest.getAttempts() < MAX_ATTEMPTS) {
                requestQueue.offer(postRequest);
                if (this.threadCount < MAX_THREADS) {
                    startThread();
                }
            } else {
                finished = true;
            }
            postRequest.setAttempts(postRequest.getAttempts() + 1);
        }
    }

    /**
     * Starts a thread to handle resending a failed POST request. The maximum number of threads is determined by
     * {@code MAX_THREADS}.
     */
    private void startThread() {
        log.info("Starting thread {}", this.threadCount);
        this.threadCount++;
        Thread thread = new Thread(() -> {
            while (true) {
                PostRequest postRequest = requestQueue.poll();
                if (postRequest != null) {
                    try {
                        Thread.sleep(postRequest.getWaitTime());
                    } catch (InterruptedException e) {
                        log.error("Sleep interrupted while waiting to resend post: ", e);
                    }
                    sendRequest(postRequest);
                } else {
                    log.info("Ending thread {}", this.threadCount);
                    this.threadCount--;
                    break;
                }
            }
        });
        thread.start();
    }
}
