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
        return GenerationPayloadFactory.createPayload(
                generationMode, processedPositivePrompt, processedNegativePrompt,
                model, width, height, scale, sampler, steps, nSamples, seed,
                smea, smeaDyn, base64Image, strength, extraNoiseSeed
        );
    }
}