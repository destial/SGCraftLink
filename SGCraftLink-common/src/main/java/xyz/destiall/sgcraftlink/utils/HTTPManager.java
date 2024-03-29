package xyz.destiall.sgcraftlink.utils;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.proxy.misc.JSONContentProvider;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HTTPManager {
    private final HttpClient client;
    private final Stack<Pair<Request, CompletableFuture<DataResult>>> queueStack;
    private final ExecutorService service;
    private boolean stopping = false;

    public HTTPManager() {
        client = new HttpClient(new SslContextFactory(true));
        try {
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        queueStack = new Stack<>();
        service = Executors.newSingleThreadExecutor();
        service.submit(() -> {
            while (!stopping) {
                if (!queueStack.isEmpty()) {
                    Pair<Request, CompletableFuture<DataResult>> pair = queueStack.pop();
                    try {
                        ContentResponse response = pair.getKey().timeout(10L, TimeUnit.SECONDS).send();
                        pair.getValue().complete(new DataResult(response));
                    } catch (Exception e) {
                        pair.getValue().completeExceptionally(e);
                    }
                }
            }
        });
    }

    public void shutdown() {
        stopping = true;
        try {
            while (!queueStack.isEmpty()) {
                Pair<Request, CompletableFuture<DataResult>> pair = queueStack.pop();
                try {
                    ContentResponse response = pair.getKey().timeout(10L, TimeUnit.SECONDS).send();
                    pair.getValue().complete(new DataResult(response));
                } catch (Exception e) {
                    pair.getValue().completeExceptionally(e);
                }
            }
            service.awaitTermination(1L, TimeUnit.SECONDS);
            client.stop();
            queueStack.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<DataResult> fetch(String url, Method method, JSONObject... body) {
        Request builder = client.newRequest(url).method(method.name());
        return buildRequest(builder, method, Collections.emptyMap(), body);
    }

    public CompletableFuture<DataResult> fetch(String url, Method method, Map<String, String> headers, JSONObject... body) {
        Request builder = client.newRequest(url).method(method.name());
        return buildRequest(builder, method, headers, body);
    }

    public CompletableFuture<DataResult> fetch(String url, Method method, Map<String, String> headers, Map<String, String> cookies, JSONObject... body) {
        Request builder = client.newRequest(url).method(method.name());
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            builder.cookie(new HttpCookie(entry.getKey(), entry.getValue()));
        }
        return buildRequest(builder, method, headers, body);
    }

    public CompletableFuture<DataResult> fetch(String url, int port, Method method, Map<String, String> headers, JSONObject... body) {
        Request builder = client.newRequest(url).method(method.name()).port(port);
        return buildRequest(builder, method, headers, body);
    }

    public CompletableFuture<DataResult> fetch(String url, int port, Method method, Map<String, String> headers, Map<String, String> cookies, JSONObject... body) {
        Request builder = client.newRequest(url).method(method.name()).port(port);
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            builder.cookie(new HttpCookie(entry.getKey(), entry.getValue()));
        }
        return buildRequest(builder, method, headers, body);
    }

    private CompletableFuture<DataResult> buildRequest(Request builder, Method method, Map<String, String> headers, JSONObject... body) {
        if (!headers.isEmpty()) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                builder.getHeaders().add(header.getKey(), header.getValue());
            }
        }

        CompletableFuture<DataResult> future = new CompletableFuture<>();
        switch (method) {
            case PUT:
            case PATCH:
            case POST: {
                if (body == null || body.length == 0) {
                    future.completeExceptionally(new RuntimeException("JSON body cannot be empty for " + method + " method!"));
                    return future;
                }
                builder.content(new JSONContentProvider(body[0]));
                break;
            }
            case GET:
            case DELETE:
                break;
        }
        Pair<Request, CompletableFuture<DataResult>> pair = new Pair<>(builder, future);
        queueStack.push(pair);
        return future;
    }

    public static Map<String, String> asMap(String key, String... values) {
        Map<String, String> map = new HashMap<>();
        if (values.length == 0) {
            return map;
        }
        List<String> list = new ArrayList<>();
        list.add(key);
        list.addAll(Arrays.asList(values));
        for (int i = 0; i < values.length; i+=2) {
            map.put(list.get(i), list.get(i + 1));
        }
        return map;
    }

    public enum Method {
        GET,
        POST,
        PUT,
        PATCH,
        DELETE
    }
}
