package com.zxzinn.novelai.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Log4j2
public class APIClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final Endpoint endpoint;
    private static final int MAX_IMAGE_PREVIEW_LENGTH = 100;

    public APIClient(Endpoint endpoint) {
        this.endpoint = Objects.requireNonNull(endpoint, "Endpoint cannot be null");
        this.httpClient = createHttpClient();
        log.info("APIClient initialized with endpoint: {}", endpoint.getUrl());
    }

    @NotNull
    private OkHttpClient createHttpClient() {
        log.debug("Creating HTTP client...");
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

        OkHttpClient client = builder.build();
        log.debug("HTTP client created successfully");
        return client;
    }

    public byte[] generateImage(GenerationPayload payload, String apiKey) throws IOException {
        log.info("開始生成圖片...");
        validateInputs(payload, apiKey);
        Request request = createRequest(payload, apiKey);
        return getResponse(request);
    }

    private void validateInputs(GenerationPayload payload, String apiKey) {
        log.debug("驗證輸入參數...");
        if (payload == null) {
            log.error("生成負載不能為空");
            throw new IllegalArgumentException("生成負載不能為空");
        }
        if (StringUtils.isBlank(apiKey)) {
            log.error("API 金鑰不能為空");
            throw new IllegalArgumentException("API 金鑰不能為空");
        }
        log.debug("輸入參數驗證通過");
    }

    @NotNull
    private Request createRequest(GenerationPayload payload, String apiKey) {
        log.debug("創建 HTTP 請求...");
        Gson gson = new GsonBuilder().create();
        String jsonPayload = gson.toJson(payload);
        RequestBody body = RequestBody.create(jsonPayload, JSON);

        String logJsonPayload = createLogFriendlyJson(payload);
        log.debug("發送的payload:\n{}", logJsonPayload);

        Request request = new Request.Builder()
                .url(endpoint.getUrl())
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        log.debug("HTTP 請求創建成功");
        return request;
    }

    private String createLogFriendlyJson(GenerationPayload payload) {
        log.debug("創建日誌友好的 JSON...");
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
                        log.debug("圖片數據已被截斷以便於日誌記錄");
                    }
                }
            }
        }

        return gson.toJson(jsonElement);
    }

    @NotNull
    private byte[] getResponse(Request request) throws IOException {
        log.info("發送請求並獲取響應...");
        try (Response response = httpClient.newCall(request).execute()) {
            log.debug("收到響應，狀態碼: {}", response.code());

            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                log.error("請求失敗，狀態碼: {}, 響應內容: {}", response.code(), responseBody);
                log.error("請求 URL: {}", request.url());
                log.error("請求方法: {}", request.method());
                log.error("請求頭: {}", request.headers());
                throw new IOException("Unexpected code " + response + "\nResponse body: " + responseBody);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                log.error("響應體為空");
                throw new IOException("Response body is null");
            }

            byte[] responseBytes = responseBody.bytes();
            log.info("成功接收響應，數據大小: {} bytes", responseBytes.length);
            return responseBytes;
        } catch (IOException e) {
            log.error("獲取響應時發生錯誤", e);
            throw e;
        }
    }
}