package com.hkhamm;

import static org.assertj.core.api.Assertions.assertThat;

import org.asynchttpclient.Response;

import org.mockserver.integration.ClientAndServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.testng.annotations.Test;

import java.util.concurrent.CompletableFuture;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Unit tests for {@link PostSender}.
 */
public class PostSenderTest {
    private ClientAndServer mockServer;
    private static final Logger log = LoggerFactory.getLogger(PostSenderTest.class);

    // Sends a POST request to a {@code mockServer} that always responds with status code 200.
    @Test
    public void testSendStatus200() throws Exception {
        mockServer = startClientAndServer(1080);
        mockServer.when(request().withMethod("POST").withPath("/")).respond(response().withStatusCode(200));

        PostSender postSender = new PostSender("http://localhost:1080");
        CompletableFuture<Response> futureResponse = postSender.send("testSendStatus200");
        Response response = futureResponse.get();

        while (!postSender.isFinished()) {
            Thread.sleep(1000);
        }

        assertThat(response.getStatusCode()).isBetween(200, 300);
        mockServer.stop();
    }

    // Sends a POST request to a {@code mockServer} that always responds with status code 300. Will retry up to
    // {@link PostSender}'s maximum attempts. WARNING: takes a long time to complete as the wait time increases
    // exponentially and the maximum wait time is 1024 seconds.
    @Test
    public void testSendStatus300() throws Exception {
        mockServer = startClientAndServer(1081);
        mockServer.when(request().withMethod("POST").withPath("/")).respond(response().withStatusCode(300));

        PostSender postSender = new PostSender("http://localhost:1081");
        CompletableFuture<Response> futureResponse = postSender.send("testSendStatus300");
        Response response = futureResponse.get();

        assertThat(response.getStatusCode()).isBetween(300, 400);

        while (!postSender.isFinished()) {
            Thread.sleep(1000);
        }

        mockServer.stop();
    }

    // Attempts to send 11 POST requests to a {@code mockServer} that always responds with status code 300. Tests that
    // the size of the queue does not exceed 10.
    @Test
    public void testSendQueueSize() throws Exception {
        mockServer = startClientAndServer(1082);
        mockServer.when(request().withMethod("POST").withPath("/")).respond(response().withStatusCode(300));

        PostSender postSender = new PostSender("http://localhost:1082");

        for (int i = 0; i < 11; i++) {
            CompletableFuture<Response> futureResponse = postSender.send("testSendQueueSize" + i);
            Response response = futureResponse.get();
            assertThat(response.getStatusCode()).isBetween(300, 400);
        }

        Thread.sleep(1000);

        assertThat(postSender.getRequestQueue().size()).isLessThan(11);

        mockServer.stop();
    }

    // Attempts to send 11 POST requests to a {@code mockServer} that always responds with status code 300. Tests that
    // the thread count does not exceed 10.
    @Test
    public void testSendThreadCount() throws Exception {
        mockServer = startClientAndServer(1083);
        mockServer.when(request().withMethod("POST").withPath("/")).respond(response().withStatusCode(300));

        PostSender postSender = new PostSender("http://localhost:1083");

        for (int i = 0; i < 5; i++) {
            CompletableFuture<Response> futureResponse = postSender.send("testSendThreadCount");
            Response response = futureResponse.get();
            assertThat(response.getStatusCode()).isBetween(300, 400);
        }

        Thread.sleep(1000);

        assertThat(postSender.getThreadCount()).isLessThan(4);

        mockServer.stop();
    }
}