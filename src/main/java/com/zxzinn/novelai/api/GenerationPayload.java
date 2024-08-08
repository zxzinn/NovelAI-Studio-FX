package com.zxzinn.novelai.api;

import lombok.Data;

@Data
public abstract class GenerationPayload {
    public String input;
    public String model;
    public String action;
    public GenerationParameters parameters;

    @Data
    public static class GenerationParameters {
        public int params_version;
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