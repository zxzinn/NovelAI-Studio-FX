package com.zxzinn.novelai.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

@Log4j2
public class APIClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final Endpoint endpoint;
    private static final int MAX_IMAGE_PREVIEW_LENGTH = 100; // 設置預覽長度

    public APIClient(Endpoint endpoint) {
        this.endpoint = endpoint;
        this.httpClient = createHttpClient();
    }

    @NotNull
    private OkHttpClient createHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS);

        ClientProxy.detectProxy().ifPresentOrElse(
                proxy -> {
                    builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort())));
                    log.info("使用代理: {}:{}", proxy.getHost(), proxy.getPort());
                },
                () -> log.info("不使用代理")
        );

        return builder.build();
    }

    public byte[] generateImage(GenerationPayload payload, String apiKey) throws IOException {
        Request request = createRequest(payload, apiKey);
        return getResponse(request);
    }

    @NotNull
    private Request createRequest(GenerationPayload payload, String apiKey) {
        Gson gson = new GsonBuilder().create();
        String jsonPayload = gson.toJson(payload);
        RequestBody body = RequestBody.create(jsonPayload, JSON);

        // 為日誌創建一個修改過的 JSON
        String logJsonPayload = createLogFriendlyJson(payload);
        log.debug("發送的payload:\n{}", logJsonPayload);

        return new Request.Builder()
                .url(endpoint.getUrl())
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();
    }

    private String createLogFriendlyJson(GenerationPayload payload) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement jsonElement = gson.toJsonTree(payload);

        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject.has("parameters") && jsonObject.get("parameters").isJsonObject()) {
                JsonObject parameters = jsonObject.getAsJsonObject("parameters");
                if (parameters.has("image")) {
                    String imageValue = parameters.get("image").getAsString();
                    if (imageValue.length() > MAX_IMAGE_PREVIEW_LENGTH) {
                        parameters.addProperty("image", imageValue.substring(0, MAX_IMAGE_PREVIEW_LENGTH) + "...(省略)");
                    }
                }
            }
        }

        return gson.toJson(jsonElement);
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