package com.zxzinn.novelai.api;

import lombok.Data;

@Data
public class ImageGenerationPayload {
    public String input;
    public String model;
    public String action;
    public ImageGenerationParameters parameters;

    @Data
    public static class ImageGenerationParameters {
        public int params_version = 1;
        public int width;
        public int height;
        public int scale;
        public String sampler;
        public int steps;
        public int n_samples;
        public boolean ucPreset;
        public boolean qualityToggle;
        public boolean sm;
        public boolean sm_dyn;
        public long seed;
        public String negative_prompt;
    }
}