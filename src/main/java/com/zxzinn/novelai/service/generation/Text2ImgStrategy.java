package com.zxzinn.novelai.service.generation;

import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.api.ImageGenerationPayload;

public class Text2ImgStrategy implements GenerationStrategy {
    @Override
    public GenerationPayload createPayload(GenerationParameters params) {
        ImageGenerationPayload payload = new ImageGenerationPayload();
        payload.setInput(params.getProcessedPositivePrompt());
        payload.setModel(params.getModel());
        payload.setAction("generate");

        GenerationPayload.GenerationParameters parameters = new GenerationPayload.GenerationParameters();
        parameters.setWidth(params.getWidth());
        parameters.setHeight(params.getHeight());
        parameters.setScale(params.getScale());
        parameters.setSampler(params.getSampler());
        parameters.setSteps(params.getSteps());
        parameters.setN_samples(params.getNSamples());
        parameters.setUcPreset(false);
        parameters.setQualityToggle(false);
        parameters.setSm(params.isSmea());
        parameters.setSm_dyn(params.isSmeaDyn());
        parameters.setSeed(params.getSeed());
        parameters.setNegative_prompt(params.getProcessedNegativePrompt());

        payload.setParameters(parameters);
        return payload;
    }
}