package com.zxzinn.novelai.api;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

@Log4j2
public class APIClient {
    private static final String API_URL = "https://api.novelai.net/ai/generate-image";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;

    public APIClient() {
        this.httpClient = createHttpClient();
    }

    @NotNull
    private OkHttpClient createHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS);

        // 檢查是否可以連接到 Clash 代理
        if (isClashProxyAvailable()) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7890));
            builder.proxy(proxy);
            log.info("使用 Clash 代理: 127.0.0.1:7890");
        } else {
            log.info("未使用代理");
        }

        return builder.build();
    }

    private boolean isClashProxyAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 7890), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public byte[] generateImage(GenerationPayload payload, String apiKey) throws IOException {
        Request request = createRequest(payload, apiKey);
        return getResponse(request);
    }

    @NotNull
    private Request createRequest(GenerationPayload payload, String apiKey) {
        Gson gson = new Gson();
        RequestBody body = RequestBody.create(gson.toJson(payload), JSON);
        return new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();
    }

    @NotNull
    private byte[] getResponse(Request request) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            ResponseBody responseBody = response.body();
            if (responseBody == null) throw new IOException("Response body is null");

            return responseBody.bytes();
        }
    }
}