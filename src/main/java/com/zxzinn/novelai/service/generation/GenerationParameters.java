package com.zxzinn.novelai.service.generation;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenerationParameters {
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
}