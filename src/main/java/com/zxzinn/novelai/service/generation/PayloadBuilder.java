package com.zxzinn.novelai.service.generation;

import com.zxzinn.novelai.api.GenerationPayload;
import lombok.Builder;

@Builder
public class PayloadBuilder {
    private String generationMode;
    private String processedPositivePrompt;
    private String processedNegativePrompt;
    private String model;
    private int width;
    private int height;
    private int scale;
    private String sampler;
    private int steps;
    private int nSamples;
    private long seed;
    private boolean smea;
    private boolean smeaDyn;
    private String base64Image;
    private double strength;
    private long extraNoiseSeed;

    public GenerationPayload createPayload() {
        GenerationParameters params = GenerationParameters.builder()
                .processedPositivePrompt(processedPositivePrompt)
                .processedNegativePrompt(processedNegativePrompt)
                .model(model)
                .width(width)
                .height(height)
                .scale(scale)
                .sampler(sampler)
                .steps(steps)
                .nSamples(nSamples)
                .seed(seed)
                .smea(smea)
                .smeaDyn(smeaDyn)
                .base64Image(base64Image)
                .strength(strength)
                .extraNoiseSeed(extraNoiseSeed)
                .build();

        return GenerationPayloadFactory.createPayload(generationMode, params);
    }
}