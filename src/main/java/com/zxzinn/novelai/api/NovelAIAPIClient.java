package com.zxzinn.novelai.api;

import com.google.gson.Gson;
import com.zxzinn.novelai.controller.ImageGeneratorController;
import com.zxzinn.novelai.controller.Img2ImgGeneratorController;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Log4j2
public class NovelAIAPIClient {
    private static final String API_URL = "https://api.novelai.net/ai/generate-image";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();

    public byte[] generateImage(ImageGeneratorController controller) throws IOException {
        ImageGenerationPayload payload = createImageGenerationPayload(controller);
        Request request = createRequest(payload, controller.apiKeyField.getText());
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Response body is null");
            }
            return body.bytes();
        }
    }

    public byte[] generateImg2Img(Img2ImgGeneratorController controller) throws IOException {
        Img2ImgGenerationPayload payload = createImg2ImgGenerationPayload(controller);
        Request request = createRequest(payload, controller.apiKeyField.getText());
        return sendRequest(request);
    }

    private ImageGenerationPayload createImageGenerationPayload(ImageGeneratorController controller) {
        ImageGenerationPayload payload = new ImageGenerationPayload();
        payload.setInput(controller.positivePromptPreviewArea.getText());
        payload.setModel(controller.modelComboBox.getValue());
        payload.setAction("generate");

        ImageGenerationPayload.ImageGenerationParameters parameters = new ImageGenerationPayload.ImageGenerationParameters();
        parameters.setWidth(Integer.parseInt(controller.widthField.getText()));
        parameters.setHeight(Integer.parseInt(controller.heightField.getText()));
        parameters.setScale(Integer.parseInt(controller.ratioField.getText()));
        parameters.setSampler(controller.samplerComboBox.getValue());
        parameters.setSteps(Integer.parseInt(controller.stepsField.getText()));
        parameters.setN_samples(Integer.parseInt(controller.countField.getText()));
        parameters.setUcPreset(false);
        parameters.setQualityToggle(false);
        parameters.setSm(controller.smeaCheckBox.isSelected());
        parameters.setSm_dyn(controller.smeaDynCheckBox.isSelected());
        parameters.setSeed(Long.parseLong(controller.seedField.getText()));
        parameters.setNegative_prompt(controller.negativePromptPreviewArea.getText());

        payload.setParameters(parameters);
        return payload;
    }

    private Img2ImgGenerationPayload createImg2ImgGenerationPayload(Img2ImgGeneratorController controller) {
        Img2ImgGenerationPayload payload = new Img2ImgGenerationPayload();
        payload.setInput(controller.positivePromptPreviewArea.getText());
        payload.setModel(controller.modelComboBox.getValue());
        payload.setAction("img2img");

        Img2ImgGenerationPayload.Img2ImgGenerationParameters parameters = new Img2ImgGenerationPayload.Img2ImgGenerationParameters();
        parameters.setWidth(Integer.parseInt(controller.widthField.getText()));
        parameters.setHeight(Integer.parseInt(controller.heightField.getText()));
        parameters.setScale(Integer.parseInt(controller.ratioField.getText()));
        parameters.setSampler(controller.samplerComboBox.getValue());
        parameters.setSteps(Integer.parseInt(controller.stepsField.getText()));
        parameters.setN_samples(Integer.parseInt(controller.countField.getText()));
        parameters.setUcPreset(false);
        parameters.setQualityToggle(false);
        parameters.setSm(controller.smeaCheckBox.isSelected());
        parameters.setSm_dyn(controller.smeaDynCheckBox.isSelected());
        parameters.setSeed(Long.parseLong(controller.seedField.getText()));
        parameters.setNegative_prompt(controller.negativePromptPreviewArea.getText());
        parameters.setImage(controller.base64Image);
        parameters.setExtra_noise_seed(Long.parseLong(controller.extraNoiseSeedField.getText()));

        payload.setParameters(parameters);
        return payload;
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

            assert response.body() != null;
            return response.body().bytes();
        }
    }
}