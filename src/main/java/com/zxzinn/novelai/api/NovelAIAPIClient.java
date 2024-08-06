package com.zxzinn.novelai.api;

import com.google.gson.Gson;
import com.zxzinn.novelai.controller.AbstractGenerationController;
import com.zxzinn.novelai.controller.Img2ImgGeneratorController;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Log4j2
public class NovelAIAPIClient implements APIClient {
    private static final String API_URL = "https://api.novelai.net/ai/generate-image";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Gson gson;

    public NovelAIAPIClient(OkHttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    @Override
    public byte[] generateImage(ImageGenerationPayload payload, String apiKey) throws IOException {
        Request request = createRequest(payload, apiKey);
        return sendRequest(request);
    }

    @Override
    public byte[] generateImg2Img(Img2ImgGenerationPayload payload, String apiKey) throws IOException {
        Request request = createRequest(payload, apiKey);
        return sendRequest(request);
    }

    private Request createRequest(Object payload, String apiKey) {
        RequestBody body = RequestBody.create(gson.toJson(payload), JSON);
        return new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();
    }

    private byte[] sendRequest(Request request) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            ResponseBody responseBody = response.body();
            if (responseBody == null) throw new IOException("Response body is null");

            return responseBody.bytes();
        }
    }
}