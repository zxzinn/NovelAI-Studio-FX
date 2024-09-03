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
        public boolean dynamic_thresholding;
        public double controlnet_strength = 1;
        public boolean legacy;
        public boolean add_original_image = true;
        public int cfg_rescale;
        public String noise_schedule;
        public boolean legacy_v3_extend;
        public long seed;
        public String negative_prompt;
    }
}