package xyz.destiall.sgcraftlink.utils;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.proxy.misc.JSONContentProvider;
import xyz.destiall.sgcraftlink.proxy.misc.Pair;

import java.util.ArrayList;
import java.util.Arrays;
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
        client = new HttpClient();
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
            service.awaitTermination(3L, TimeUnit.SECONDS);
            client.stop();
            queueStack.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<DataResult> fetch(String url, int port, Method method, Map<String, String> headers, JSONObject... body) {
        Request builder = client.newRequest(url).method(method.name()).port(port);
        if (!headers.isEmpty()) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                builder.getHeaders().add(header.getKey(), header.getValue());
            }
        }
        switch (method) {
            case PUT:
            case PATCH:
            case POST:{
                builder.content(new JSONContentProvider(body[0]));
                break;
            }
            case GET:
            case DELETE:
                break;
        }
        CompletableFuture<DataResult> future = new CompletableFuture<>();
        Pair<Request, CompletableFuture<DataResult>> pair = new Pair<>(builder, future);
        queueStack.push(pair);
        return future;
    }

    public static Map<String, String> headers(String key, String... values) {
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
