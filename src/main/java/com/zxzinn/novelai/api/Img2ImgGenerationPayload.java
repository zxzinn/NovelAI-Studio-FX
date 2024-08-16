package com.zxzinn.novelai.api;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Img2ImgGenerationPayload extends GenerationPayload {
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Img2ImgGenerationParameters extends GenerationParameters {
        private double strength = 0.5;
        private int noise = 0;
        private boolean dynamic_thresholding = false;
        private double controlnet_strength = 1.0;
        private boolean legacy = false;
        private boolean add_original_image = true;
        private int cfg_rescale = 0;
        private String noise_schedule = "native";
        private boolean legacy_v3_extend = false;
        private String image;
        private long extra_noise_seed;
    }
}