package com.zxzinn.novelai.service.generation;

import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.api.Img2ImgGenerationPayload;

public class Img2ImgStrategy implements GenerationStrategy {
    @Override
    public GenerationPayload createPayload(GenerationParameters params) {
        Img2ImgGenerationPayload payload = new Img2ImgGenerationPayload();
        payload.setInput(params.getProcessedPositivePrompt());
        payload.setModel(params.getModel());
        payload.setAction("img2img");

        Img2ImgGenerationPayload.Img2ImgGenerationParameters parameters = new Img2ImgGenerationPayload.Img2ImgGenerationParameters();
        parameters.setWidth(params.getWidth());
        parameters.setHeight(params.getHeight());
        parameters.setScale(params.getScale());
        parameters.setSampler(params.getSampler());
        parameters.setSteps(params.getSteps());
        parameters.setN_samples(params.getNSamples());
        parameters.setUcPreset(false);
        parameters.setQualityToggle(false);
        parameters.setSeed(params.getSeed());
        parameters.setNegative_prompt(params.getProcessedNegativePrompt());
        parameters.setImage(params.getBase64Image());
        parameters.setExtra_noise_seed(params.getExtraNoiseSeed());

        parameters.setStrength(params.getStrength());
        parameters.setNoise(0);
        parameters.setDynamic_thresholding(false);
        parameters.setControlnet_strength(1.0);
        parameters.setLegacy(false);
        parameters.setAdd_original_image(true);
        parameters.setCfg_rescale(0);
        parameters.setNoise_schedule("native");
        parameters.setLegacy_v3_extend(false);

        payload.setParameters(parameters);
        return payload;
    }
}